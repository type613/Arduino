void setup(){
  pinMode(4,OUTPUT);
  pinMode(5,OUTPUT);
  pinMode(9,OUTPUT);
  pinMode(6,OUTPUT);
  pinMode(7,OUTPUT);
  pinMode(10,OUTPUT);
}

void loop(){
  //Moter 1
  analogWrite(9,160);
  analogWrite(10,160);
  digitalWrite(4,LOW);
  digitalWrite(5,HIGH);
  digitalWrite(6,LOW);
  digitalWrite(7,HIGH);

  delay(1000);

  analogWrite(9,0);
  analogWrite(10,0);
  delay(1000);

  analogWrite(9,40);
  analogWrite(10,40);
  digitalWrite(4,HIGH);
  digitalWrite(5,LOW);
  digitalWrite(6,HIGH);
  digitalWrite(7,LOW);
  delay(1000);


  //Moter 2
  analogWrite(9,0);
  analogWrite(10,0);
  delay(1000);

  analogWrite(10,160);
  digitalWrite(6,LOW);
  digitalWrite(7,HIGH); 
  delay(1000);

  analogWrite(10,255);
  digitalWrite(6,HIGH);
  digitalWrite(7,HIGH);
  delay(1000);

  analogWrite(10,40);
  digitalWrite(6,HIGH);
  digitalWrite(7,LOW);
  delay(1000);

  analogWrite(10,0);
  delay(1000);

}


