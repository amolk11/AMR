package com.amol.amr.detection

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import com.amol.amr.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import kotlin.math.max
import kotlin.math.min

class TFLitePersonDetector(
    private val context: Context,
    private val modelPath: String = "models/yolov8n_float32.tflite",
    private val inputSize: Int = 640,
    private val confThreshold: Float = 0.45f,
    private val iouThreshold: Float = 0.50f,
    private val numThreads: Int = 4
) : YoloDetector {

    private var interpreter: Interpreter? = null
    private var gpuDelegate: GpuDelegate? = null

    // Preallocated buffers for zero-allocation per frame
    private val inputBuffer: ByteBuffer = ByteBuffer.allocateDirect(1 * inputSize * inputSize * 3 * 4).apply {
        order(ByteOrder.nativeOrder())
    }
    private val intValues = IntArray(inputSize * inputSize)

    init {
        initializeInterpreter()
    }

    private fun initializeInterpreter() {
        try {
            val modelBuffer = loadModelFile(context, modelPath)
            val options = Interpreter.Options().apply {
                setNumThreads(numThreads)
                try {
                    val compatList = CompatibilityList()
                    if (compatList.isDelegateSupportedOnThisDevice) {
                        val delegateOptions = compatList.bestOptionsForThisDevice
                        gpuDelegate = GpuDelegate(delegateOptions)
                        addDelegate(gpuDelegate)
                        Logger.i("TFLitePersonDetector", "GPU acceleration enabled successfully.")
                    } else {
                        setUseNNAPI(true)
                        Logger.i("TFLitePersonDetector", "NNAPI fallback enabled.")
                    }
                } catch (t: Throwable) {
                    Logger.w("TFLitePersonDetector", "GPU/NNAPI initialization failed, using CPU: ${t.message}")
                    setNumThreads(numThreads)
                }
            }
            interpreter = Interpreter(modelBuffer, options)
            Logger.i("TFLitePersonDetector", "TFLite interpreter loaded from $modelPath")
        } catch (e: Exception) {
            Logger.e("TFLitePersonDetector", "Failed to initialize TFLite model, using CPU mode fallback", e)
            try {
                val fallbackOptions = Interpreter.Options().apply { setNumThreads(numThreads) }
                val modelBuffer = loadModelFile(context, modelPath)
                interpreter = Interpreter(modelBuffer, fallbackOptions)
            } catch (fallbackEx: Exception) {
                Logger.e("TFLitePersonDetector", "Critical failure loading model file $modelPath", fallbackEx)
            }
        }
    }

    private fun loadModelFile(context: Context, path: String): ByteBuffer {
        val fileDescriptor = context.assets.openFd(path)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    override suspend fun detect(bitmap: Bitmap, rotationDegrees: Int): DetectionFrame = withContext(Dispatchers.Default) {
        val startTime = SystemClock.elapsedRealtime()
        val originalW = bitmap.width
        val originalH = bitmap.height

        // 1. Rotate and Resize bitmap to model input size (640x640)
        val matrix = Matrix()
        if (rotationDegrees != 0) {
            matrix.postRotate(rotationDegrees.toFloat())
        }
        val rotatedBitmap = if (rotationDegrees != 0) {
            Bitmap.createBitmap(bitmap, 0, 0, originalW, originalH, matrix, true)
        } else {
            bitmap
        }

        val frameW = rotatedBitmap.width
        val frameH = rotatedBitmap.height

        val scaledBitmap = Bitmap.createScaledBitmap(rotatedBitmap, inputSize, inputSize, true)

        // 2. Preprocess into FloatBuffer [1, 640, 640, 3] normalized [0.0 .. 1.0]
        inputBuffer.rewind()
        scaledBitmap.getPixels(intValues, 0, inputSize, 0, 0, inputSize, inputSize)

        for (pixel in intValues) {
            val r = ((pixel shr 16) and 0xFF) / 255.0f
            val g = ((pixel shr 8) and 0xFF) / 255.0f
            val b = (pixel and 0xFF) / 255.0f
            inputBuffer.putFloat(r)
            inputBuffer.putFloat(g)
            inputBuffer.putFloat(b)
        }

        // 3. Output shape for YOLOv8: [1, 84, 8400] (for 80 classes + 4 bbox coords) or [1, 5, 8400]
        val outputArray = Array(1) { Array(84) { FloatArray(8400) } }
        val outputMap = mapOf(0 to outputArray)

        interpreter?.runForMultipleInputsOutputs(arrayOf(inputBuffer), outputMap)

        // 4. Post-process detections for Person Class (Class Index 0)
        val rawCandidates = mutableListOf<Detection>()
        val numBoxes = 8400

        for (i in 0 until numBoxes) {
            val cx = outputArray[0][0][i]
            val cy = outputArray[0][1][i]
            val w = outputArray[0][2][i]
            val h = outputArray[0][3][i]
            val personConf = outputArray[0][4][i] // Class 0 confidence

            if (personConf >= confThreshold) {
                val x1Norm = (cx - w / 2.0f) / inputSize.toFloat()
                val y1Norm = (cy - h / 2.0f) / inputSize.toFloat()
                val x2Norm = (cx + w / 2.0f) / inputSize.toFloat()
                val y2Norm = (cy + h / 2.0f) / inputSize.toFloat()

                val x1 = (x1Norm * frameW).coerceIn(0.0f, frameW.toFloat())
                val y1 = (y1Norm * frameH).coerceIn(0.0f, frameH.toFloat())
                val x2 = (x2Norm * frameW).coerceIn(0.0f, frameW.toFloat())
                val y2 = (y2Norm * frameH).coerceIn(0.0f, frameH.toFloat())

                if (x2 > x1 && y2 > y1) {
                    rawCandidates.add(
                        Detection(
                            classId = 0,
                            label = "person",
                            confidence = personConf,
                            boundingBox = BoundingBox(x1, y1, x2, y2)
                        )
                    )
                }
            }
        }

        // 5. Non-Maximum Suppression (NMS)
        val finalDetections = applyNms(rawCandidates, iouThreshold)
        val inferenceDuration = (SystemClock.elapsedRealtime() - startTime).toFloat()

        DetectionFrame(
            detections = finalDetections,
            inferenceTimeMs = inferenceDuration,
            frameWidth = frameW,
            frameHeight = frameH,
            timestampMs = System.currentTimeMillis()
        )
    }

    private fun applyNms(detections: List<Detection>, iouThreshold: Float): List<Detection> {
        val sorted = detections.sortedByDescending { it.confidence }
        val selected = mutableListOf<Detection>()

        for (candidate in sorted) {
            var shouldSelect = true
            for (chosen in selected) {
                if (computeIoU(candidate.boundingBox, chosen.boundingBox) > iouThreshold) {
                    shouldSelect = false
                    break
                }
            }
            if (shouldSelect) {
                selected.add(candidate)
            }
        }
        return selected
    }

    private fun computeIoU(b1: BoundingBox, b2: BoundingBox): Float {
        val interX1 = max(b1.x1, b2.x1)
        val interY1 = max(b1.y1, b2.y1)
        val interX2 = min(b1.x2, b2.x2)
        val interY2 = min(b1.y2, b2.y2)

        val interW = max(0.0f, interX2 - interX1)
        val interH = max(0.0f, interY2 - interY1)
        val interArea = interW * interH

        val unionArea = b1.area + b2.area - interArea
        return if (unionArea > 0.0f) interArea / unionArea else 0.0f
    }

    override fun close() {
        interpreter?.close()
        gpuDelegate?.close()
        interpreter = null
        gpuDelegate = null
    }
}
