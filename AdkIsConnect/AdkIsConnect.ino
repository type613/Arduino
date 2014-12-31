#include <Max3421e.h>
#include <Usb.h>
#include <AndroidAccessory.h>

AndroidAccessory acc("Dorobook,Socym",
"AdbNoSuchAppTitle",
"このAndroidはADKが使えます ADK Connect Test",
"1.0",
"http://accssories.android.com/",
"0000000012345678");

void setup()
{
  Serial.begin(115200);
  Serial.print("¥r¥nStart");
  acc.powerOn();
}

void loop()
{
  if (acc.isConnected())
  {
  }
  else
  {
  }
  delay(10);
}
