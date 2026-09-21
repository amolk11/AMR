#include <Arduino.h>

// ================= MOTOR DRIVER PINS =================

// LEFT SIDE
#define IN1 5
#define IN2 6

// RIGHT SIDE
#define IN3 7
#define IN4 8

// SPEED CONTROL
#define ENA 36
#define ENB 37

// ================= SPEED VALUE =================

// Range: 0 to 255
int speedValue = 120;

// ================= FUNCTION DECLARATIONS =================

void forward();
void backward();
void turnLeft();
void turnRight();
void stopMotors();

// ================= SETUP =================

void setup() {

  // MOTOR DIRECTION PINS
  pinMode(IN1, OUTPUT);
  pinMode(IN2, OUTPUT);

  pinMode(IN3, OUTPUT);
  pinMode(IN4, OUTPUT);

  // PWM PINS
  pinMode(ENA, OUTPUT);
  pinMode(ENB, OUTPUT);

  stopMotors();
}

// ================= MAIN LOOP =================

void loop() {

  // MOVE FORWARD
  forward();
  delay(3000);

  // STOP
  stopMotors();
  delay(1000);

  // MOVE BACKWARD
  backward();
  delay(3000);

  // STOP
  stopMotors();
  delay(1000);

  // TURN LEFT
  turnLeft();
  delay(2000);

  // STOP
  stopMotors();
  delay(1000);

  // TURN RIGHT
  turnRight();
  delay(2000);

  // STOP
  stopMotors();
  delay(1000);
}

// ================= MOTOR FUNCTIONS =================

// FORWARD
void forward() {

  digitalWrite(IN1, HIGH);
  digitalWrite(IN2, LOW);

  digitalWrite(IN3, HIGH);
  digitalWrite(IN4, LOW);

  analogWrite(ENA, speedValue);
  analogWrite(ENB, speedValue);
}

// BACKWARD
void backward() {

  digitalWrite(IN1, LOW);
  digitalWrite(IN2, HIGH);

  digitalWrite(IN3, LOW);
  digitalWrite(IN4, HIGH);

  analogWrite(ENA, speedValue);
  analogWrite(ENB, speedValue);
}

// LEFT TURN
void turnLeft() {

  digitalWrite(IN1, LOW);
  digitalWrite(IN2, HIGH);

  digitalWrite(IN3, HIGH);
  digitalWrite(IN4, LOW);

  analogWrite(ENA, speedValue);
  analogWrite(ENB, speedValue);
}

// RIGHT TURN
void turnRight() {

  digitalWrite(IN1, HIGH);
  digitalWrite(IN2, LOW);

  digitalWrite(IN3, LOW);
  digitalWrite(IN4, HIGH);

  analogWrite(ENA, speedValue);
  analogWrite(ENB, speedValue);
}

// STOP
void stopMotors() {

  analogWrite(ENA, 0);
  analogWrite(ENB, 0);

  digitalWrite(IN1, LOW);
  digitalWrite(IN2, LOW);

  digitalWrite(IN3, LOW);
  digitalWrite(IN4, LOW);
}