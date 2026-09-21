#include <Arduino.h>
#include <ESP32Servo.h>

// ================= MOTOR PINS =================

// LEFT MOTOR
#define IN1 14
#define IN2 15

// RIGHT MOTOR
#define IN3 17
#define IN4 18

// ENABLE PINS (PWM)
#define ENA 16
#define ENB 4

// ================= ULTRASONIC SENSOR =================

#define TRIG_PIN 21
#define ECHO_PIN 47

// ================= SERVO =================

#define SERVO_PIN 13

Servo scanServo;

// ================= PWM SETTINGS =================

#define PWM_FREQ 5000
#define PWM_RESOLUTION 8

#define PWM_CHANNEL_A 0
#define PWM_CHANNEL_B 1

// ================= SPEED =================

// Range: 0 to 255
int forwardSpeed = 180;
int turnSpeed = 200;

// ================= FUNCTION DECLARATIONS =================

void forward();
void backward();
void stopMotors();

void turnLeft();
void turnRight();

long getDistance();

void setMotorSpeed(int leftSpeed, int rightSpeed);

// ================= SETUP =================

void setup() {

  // SERIAL MONITOR
  Serial.begin(115200);

  delay(2000);

  Serial.println("Smart Robot Started");

  // ================= MOTOR PINS =================

  pinMode(IN1, OUTPUT);
  pinMode(IN2, OUTPUT);

  pinMode(IN3, OUTPUT);
  pinMode(IN4, OUTPUT);

  // ================= PWM SETUP =================

  ledcSetup(PWM_CHANNEL_A, PWM_FREQ, PWM_RESOLUTION);
  ledcSetup(PWM_CHANNEL_B, PWM_FREQ, PWM_RESOLUTION);

  ledcAttachPin(ENA, PWM_CHANNEL_A);
  ledcAttachPin(ENB, PWM_CHANNEL_B);

  // ================= ULTRASONIC =================

  pinMode(TRIG_PIN, OUTPUT);
  pinMode(ECHO_PIN, INPUT);

  // ================= SERVO =================

  scanServo.attach(SERVO_PIN);

  // INITIAL FRONT POSITION
  scanServo.write(90);

  stopMotors();
}

// ================= MAIN LOOP =================

void loop() {

  // LOOK FRONT
  scanServo.write(90);
  delay(250);

  // GET FRONT DISTANCE
  long frontDistance = getDistance();

  // PRINT FRONT DISTANCE
  Serial.print("Front Distance: ");
  Serial.println(frontDistance);

  // ================= MOVE FORWARD =================

  if (frontDistance > 20) {

    forward();

    Serial.println("FORWARD");
  }

  // ================= OBSTACLE DETECTED =================

  else {

    stopMotors();

    Serial.println("OBSTACLE DETECTED");

    delay(400);

    // ================= SCAN LEFT =================

    for (int angle = 90; angle <= 150; angle += 5) {

      scanServo.write(angle);
      delay(25);
    }

    delay(300);

    long leftDistance = getDistance();

    Serial.print("Left Distance: ");
    Serial.println(leftDistance);

    // ================= SCAN RIGHT =================

    for (int angle = 150; angle >= 30; angle -= 5) {

      scanServo.write(angle);
      delay(25);
    }

    delay(300);

    long rightDistance = getDistance();

    Serial.print("Right Distance: ");
    Serial.println(rightDistance);

    // ================= RETURN CENTER =================

    for (int angle = 30; angle <= 90; angle += 5) {

      scanServo.write(angle);
      delay(25);
    }

    delay(200);

    // ================= DECISION LOGIC =================

    // BOTH SIDES BLOCKED
    if (leftDistance < 20 && rightDistance < 20) {

      Serial.println("BOTH SIDES BLOCKED");

      backward();
      delay(700);

      stopMotors();
      delay(300);

      turnRight();
      delay(900);

      stopMotors();
    }

    // LEFT SIDE CLEARER
    else if (leftDistance > rightDistance) {

      Serial.println("TURN LEFT");

      turnLeft();
      delay(700);

      stopMotors();
    }

    // RIGHT SIDE CLEARER
    else {

      Serial.println("TURN RIGHT");

      turnRight();
      delay(700);

      stopMotors();
    }
  }

  delay(100);
}

// ================= DISTANCE FUNCTION =================

long getDistance() {

  // CLEAR TRIG
  digitalWrite(TRIG_PIN, LOW);
  delayMicroseconds(5);

  // SEND ULTRASONIC PULSE
  digitalWrite(TRIG_PIN, HIGH);
  delayMicroseconds(10);

  digitalWrite(TRIG_PIN, LOW);

  // READ ECHO SIGNAL
  long duration = pulseIn(ECHO_PIN, HIGH, 30000);

  // NO ECHO RECEIVED
  if (duration == 0) {

    return 999;
  }

  // CALCULATE DISTANCE
  long distance = duration * 0.034 / 2;

  return distance;
}

// ================= SPEED CONTROL =================

void setMotorSpeed(int leftSpeed, int rightSpeed) {

  ledcWrite(PWM_CHANNEL_A, leftSpeed);
  ledcWrite(PWM_CHANNEL_B, rightSpeed);
}

// ================= MOTOR FUNCTIONS =================

// FORWARD
void forward() {

  digitalWrite(IN1, HIGH);
  digitalWrite(IN2, LOW);

  digitalWrite(IN3, HIGH);
  digitalWrite(IN4, LOW);

  setMotorSpeed(forwardSpeed, forwardSpeed);
}

// BACKWARD
void backward() {

  digitalWrite(IN1, LOW);
  digitalWrite(IN2, HIGH);

  digitalWrite(IN3, LOW);
  digitalWrite(IN4, HIGH);

  setMotorSpeed(forwardSpeed, forwardSpeed);
}

// TURN LEFT
void turnLeft() {

  // LEFT MOTOR FORWARD
  digitalWrite(IN1, HIGH);
  digitalWrite(IN2, LOW);

  // RIGHT MOTOR BACKWARD
  digitalWrite(IN3, LOW);
  digitalWrite(IN4, HIGH);

  setMotorSpeed(turnSpeed, turnSpeed);
}

// TURN RIGHT
void turnRight() {

  // LEFT MOTOR BACKWARD
  digitalWrite(IN1, LOW);
  digitalWrite(IN2, HIGH);

  // RIGHT MOTOR FORWARD
  digitalWrite(IN3, HIGH);
  digitalWrite(IN4, LOW);

  setMotorSpeed(turnSpeed, turnSpeed);
}

// STOP
void stopMotors() {

  ledcWrite(PWM_CHANNEL_A, 0);
  ledcWrite(PWM_CHANNEL_B, 0);

  digitalWrite(IN1, LOW);
  digitalWrite(IN2, LOW);

  digitalWrite(IN3, LOW);
  digitalWrite(IN4, LOW);
}