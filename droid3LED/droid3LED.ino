/*
  Blink
  Turns on an LED on for one second, then off for one second, repeatedly.
 
  This example code is in the public domain.
 */
 
// Pin 13 has an LED connected on most Arduino boards.
// give it a name:
int red = 3;
int blue =5;
int green=6;

// the setup routine runs once when you press reset:
void setup() {                
  // initialize the digital pin as an output.
  pinMode(red, OUTPUT);
  pinMode(blue,OUTPUT);
  pinMode(green,OUTPUT);  
}

// the loop routine runs over and over again forever:
void loop() {
  analogWrite(red, 255);   // turn the LED on (HIGH is the voltage level)
  analogWrite(green, 255);   // turn the LED on (HIGH is the voltage level)
  analogWrite(blue, 255);   // turn the LED on (HIGH is the voltage level)
  delay(500);               // wait for a second

  analogWrite(red, 0);   // turn the LED on (HIGH is the voltage level)
  analogWrite(green, 0);   // turn the LED on (HIGH is the voltage level)
  analogWrite(blue, 0);   // turn the LED on (HIGH is the voltage level)
  delay(500);               // wait for a second
  for(int r=0;r<=255;r+=20)
  {
    for(int g=0;g<=255;g+=20)
    {
      for(int b=0;b<=255;b+=20)
      {

        analogWrite(red, r);    // turn the LED off by making the voltage LOW
        analogWrite(green, g);    // turn the LED off by making the voltage LOW
        analogWrite(blue, b);    // turn the LED off by making the voltage LOW
        delay(20);               // wait for a second
      }
    }
  }

    
}
