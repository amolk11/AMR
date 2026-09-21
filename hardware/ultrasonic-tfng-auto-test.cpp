#include <Arduino.h>
#include <ESP32Servo.h>

// ================= TB6612FNG MOTOR PINS =================

// LEFT MOTOR
#define AIN1 14
#define AIN2 15
#define PWMA 16

// RIGHT MOTOR
#define BIN1 17
#define BIN2 18
#define PWMB 4

// STANDBY
#define STBY 12

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

// CONTROLLED SPEEDS
int forwardSpeed = 120;
int turnSpeed = 130;
int reverseSpeed = 120;

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

  Serial.begin(115200);

  delay(2000);

  Serial.println("Controlled Autonomous Robot Started");

  // ================= MOTOR PINS =================

  pinMode(AIN1, OUTPUT);
  pinMode(AIN2, OUTPUT);

  pinMode(BIN1, OUTPUT);
  pinMode(BIN2, OUTPUT);

  pinMode(STBY, OUTPUT);

  // ENABLE DRIVER
  digitalWrite(STBY, HIGH);

  // ================= PWM SETUP =================

  ledcSetup(PWM_CHANNEL_A, PWM_FREQ, PWM_RESOLUTION);
  ledcSetup(PWM_CHANNEL_B, PWM_FREQ, PWM_RESOLUTION);

  ledcAttachPin(PWMA, PWM_CHANNEL_A);
  ledcAttachPin(PWMB, PWM_CHANNEL_B);

  // ================= ULTRASONIC =================

  pinMode(TRIG_PIN, OUTPUT);
  pinMode(ECHO_PIN, INPUT);

  // ================= SERVO =================

  scanServo.attach(SERVO_PIN);

  // CENTER POSITION
  scanServo.write(90);

  delay(500);

  stopMotors();
}

// ================= MAIN LOOP =================

void loop() {

  // FRONT DISTANCE
  long frontDistance = getDistance();

  Serial.print("Front Distance: ");
  Serial.println(frontDistance);

  // ================= FORWARD =================

  if (frontDistance > 35) {

    forward();

    Serial.println("FORWARD");
  }

  // ================= OBSTACLE =================

  else {

    stopMotors();

    Serial.println("OBSTACLE DETECTED");

    delay(500);

    // ================= LOOK LEFT =================

    for (int angle = 90; angle <= 150; angle += 5) {

      scanServo.write(angle);
      delay(35);
    }

    // STABILIZE SENSOR
    delay(450);

    long leftDistance = getDistance();

    Serial.print("Left Distance: ");
    Serial.println(leftDistance);

    // ================= LOOK RIGHT =================

    for (int angle = 150; angle >= 30; angle -= 5) {

      scanServo.write(angle);
      delay(35);
    }

    // STABILIZE SENSOR
    delay(450);

    long rightDistance = getDistance();

    Serial.print("Right Distance: ");
    Serial.println(rightDistance);

    // ================= CENTER AGAIN =================

    for (int angle = 30; angle <= 90; angle += 5) {

      scanServo.write(angle);
      delay(35);
    }

    delay(300);

    // ================= DECISION LOGIC =================

    // BOTH SIDES BLOCKED
    if (leftDistance < 20 && rightDistance < 20) {

      Serial.println("BOTH SIDES BLOCKED");

      // MOVE BACKWARD
      backward();
      delay(1100);

      stopMotors();
      delay(400);

      // TURN RIGHT
      turnRight();
      delay(850);

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

  delay(120);
}

// ================= DISTANCE FUNCTION =================

long getDistance() {

  long total = 0;

  // MULTIPLE READINGS
  for (int i = 0; i < 5; i++) {

    digitalWrite(TRIG_PIN, LOW);
    delayMicroseconds(5);

    digitalWrite(TRIG_PIN, HIGH);
    delayMicroseconds(10);

    digitalWrite(TRIG_PIN, LOW);

    long duration = pulseIn(ECHO_PIN, HIGH, 30000);

    long distance;

    if (duration == 0) {

      distance = 999;
    }

    else {

      distance = duration * 0.034 / 2;
    }

    total += distance;

    delay(15);
  }

  // AVERAGE DISTANCE
  long averageDistance = total / 5;

  return averageDistance;
}

// ================= SPEED CONTROL =================

void setMotorSpeed(int leftSpeed, int rightSpeed) {

  ledcWrite(PWM_CHANNEL_A, leftSpeed);
  ledcWrite(PWM_CHANNEL_B, rightSpeed);
}

// ================= MOTOR FUNCTIONS =================

// FORWARD
void forward() {

  digitalWrite(AIN1, HIGH);
  digitalWrite(AIN2, LOW);

  digitalWrite(BIN1, HIGH);
  digitalWrite(BIN2, LOW);

  setMotorSpeed(forwardSpeed, forwardSpeed);
}

// BACKWARD
void backward() {

  digitalWrite(AIN1, LOW);
  digitalWrite(AIN2, HIGH);

  digitalWrite(BIN1, LOW);
  digitalWrite(BIN2, HIGH);

  setMotorSpeed(reverseSpeed, reverseSpeed);
}

// TURN LEFT
void turnLeft() {

  digitalWrite(AIN1, LOW);
  digitalWrite(AIN2, HIGH);

  digitalWrite(BIN1, HIGH);
  digitalWrite(BIN2, LOW);

  setMotorSpeed(turnSpeed, turnSpeed);
}

// TURN RIGHT
void turnRight() {

  digitalWrite(AIN1, HIGH);
  digitalWrite(AIN2, LOW);

  digitalWrite(BIN1, LOW);
  digitalWrite(BIN2, HIGH);

  setMotorSpeed(turnSpeed, turnSpeed);
}

// STOP
void stopMotors() {

  ledcWrite(PWM_CHANNEL_A, 0);
  ledcWrite(PWM_CHANNEL_B, 0);

  digitalWrite(AIN1, LOW);
  digitalWrite(AIN2, LOW);

  digitalWrite(BIN1, LOW);
  digitalWrite(BIN2, LOW);
}