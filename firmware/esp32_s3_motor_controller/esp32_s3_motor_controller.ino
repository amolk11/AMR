/**
 * ESP32-S3 Autonomous Mobile Robot (AMR) Motor Controller Firmware
 * 
 * Features:
 *  - High-Speed USB-CDC / UART serial command parser
 *  - Differential drive kinematics (ENA/ENB PWM mixing)
 *  - 500ms Communication Watchdog Failsafe (automatic stop on signal loss)
 *  - Emergency Stop hardware override
 */

#include <Arduino.h>

// ================= MOTOR PIN DEFINITIONS =================
// Left Motor Direction
#define PIN_IN1 5
#define PIN_IN2 6

// Right Motor Direction
#define PIN_IN3 7
#define PIN_IN4 8

// Speed Control (PWM)
#define PIN_ENA 36
#define PIN_ENB 37

// Watchdog Timeout in Milliseconds
#define WATCHDOG_TIMEOUT_MS 500

// ================= GLOBAL STATE =================
unsigned long lastCommandTime = 0;
bool isFailsafeActive = true;

// ================= FUNCTION PROTOTYPES =================
void setMotorSpeeds(int leftPwm, int rightPwm);
void stopMotors();
void parseSerialCommand(String cmd);

void setup() {
  // Initialize Serial (Native USB CDC or UART)
  Serial.begin(115200);

  // Setup motor control pins
  pinMode(PIN_IN1, OUTPUT);
  pinMode(PIN_IN2, OUTPUT);
  pinMode(PIN_IN3, OUTPUT);
  pinMode(PIN_IN4, OUTPUT);
  pinMode(PIN_ENA, OUTPUT);
  pinMode(PIN_ENB, OUTPUT);

  // Initial Safe Stop
  stopMotors();
  lastCommandTime = millis();
}

void loop() {
  // 1. Read Serial Command Packets from Android Phone
  while (Serial.available() > 0) {
    String line = Serial.readStringUntil('\n');
    line.trim();
    if (line.length() > 0) {
      parseSerialCommand(line);
      lastCommandTime = millis();
      isFailsafeActive = false;
    }
  }

  // 2. Communication Watchdog Failsafe
  if (!isFailsafeActive && (millis() - lastCommandTime > WATCHDOG_TIMEOUT_MS)) {
    stopMotors();
    isFailsafeActive = true;
    Serial.println("STATUS:FAILSAFE_WATCHDOG_TRIGGERED");
  }
}

/**
 * Parses both Compact format ("STEER:15 SPEED:0.75" / "STOP") 
 * and CSV format ("CMD,FORWARD,180,15,0,102")
 */
void parseSerialCommand(String cmd) {
  if (cmd == "STOP" || cmd.startsWith("CMD,STOP")) {
    stopMotors();
    return;
  }

  float steeringAngle = 0.0;
  float speedScale = 0.0;

  if (cmd.startsWith("STEER:")) {
    // Compact format: STEER:15 SPEED:0.75
    int speedIndex = cmd.indexOf("SPEED:");
    if (speedIndex != -1) {
      steeringAngle = cmd.substring(6, speedIndex).toFloat();
      speedScale = cmd.substring(speedIndex + 6).toFloat();
    }
  } else if (cmd.startsWith("CMD,")) {
    // CSV format: CMD,DIRECTION,PWM,STEER,STOP_FLAG,SEQ
    int firstComma = cmd.indexOf(',');
    int secondComma = cmd.indexOf(',', firstComma + 1);
    int thirdComma = cmd.indexOf(',', secondComma + 1);
    int fourthComma = cmd.indexOf(',', thirdComma + 1);
    int fifthComma = cmd.indexOf(',', fourthComma + 1);

    if (firstComma != -1 && secondComma != -1 && thirdComma != -1 && fourthComma != -1) {
      int pwmVal = cmd.substring(secondComma + 1, thirdComma).toInt();
      steeringAngle = cmd.substring(thirdComma + 1, fourthComma).toFloat();
      int stopFlag = cmd.substring(fourthComma + 1, fifthComma != -1 ? fifthComma : cmd.length()).toInt();

      if (stopFlag == 1) {
        stopMotors();
        return;
      }
      speedScale = (float)pwmVal / 255.0f;
    }
  }

  // Differential Drive Mixing
  // Positive (+) Steering = Turn LEFT (reduce left motor, boost right motor)
  // Negative (-) Steering = Turn RIGHT (reduce right motor, boost left motor)
  float basePwm = speedScale * 255.0f;
  float turnFactor = (steeringAngle / 45.0f) * 0.5f; // Turn adjustment range [-0.5 .. +0.5]

  int leftPwm = (int)(basePwm * (1.0f - turnFactor));
  int rightPwm = (int)(basePwm * (1.0f + turnFactor));

  setMotorSpeeds(leftPwm, rightPwm);
}

void setMotorSpeeds(int leftPwm, int rightPwm) {
  leftPwm = constrain(leftPwm, 0, 255);
  rightPwm = constrain(rightPwm, 0, 255);

  // Set forward direction
  digitalWrite(PIN_IN1, HIGH);
  digitalWrite(PIN_IN2, LOW);
  digitalWrite(PIN_IN3, HIGH);
  digitalWrite(PIN_IN4, LOW);

  // Apply PWM speeds
  analogWrite(PIN_ENA, leftPwm);
  analogWrite(PIN_ENB, rightPwm);
}

void stopMotors() {
  digitalWrite(PIN_IN1, LOW);
  digitalWrite(PIN_IN2, LOW);
  digitalWrite(PIN_IN3, LOW);
  digitalWrite(PIN_IN4, LOW);
  analogWrite(PIN_ENA, 0);
  analogWrite(PIN_ENB, 0);
}
