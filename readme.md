# GIGATMS SDK for Android

Introduction
------------

The GIGA-TMS UHF SDK sample shows a list of available devices and provides an interface to connect and communication.

Pre-requisites
--------------
- Android version 5.0~9.0
- Android API 21~28
- Android Build Tools v27.0.2
- Android IDE 3.4.1
- Android Support Repository


Getting Started
---------------
This sample uses the Gradle build system. To build this project, use "Import Project" in Android Studio.


Capabilities
------------
The SDK Library has the following capabilities:

- Start and stop the inventory process.

- Read and write tag data.

- Configures the UHF reader settings.

Model
-----
The general device behavior model of the UHF reader is:

- Use StartInventory to start inventory process using specified method of triggering RF power to read tag data.

- The read tag data is presented at OnTagPresented event.

- Use StopInventory() to stop inventory process.

##### TS800 Reader

Tutorials
---------
This tutorials will give you straight-and-simple 1-2-3 style operations about the usage of SDK library.

This article don't involve too much details. If you need comprehensive understanding, please read the complete manual topic, and not just the tutorial.

TS800, TS100 and UR0250 are used in the similar way. The tutorial use TS800 as a example.

### Use TS800Scanner to scan TS800 reader

Use TS800Scanner to Scan TS800.

``` java
 TS800Scanner ts800Scanner = new TS800Scanner(Context, scannerCallback, BLE);
 ts800Scanner.setScannerCallback(scannerCallback);
 ts800Scanner.startScan();
```

When a TS800 is found, a callback function will called.
```java
 ScannerCallback scannerCallback = new ScannerCallback{
    @Override
    public void didDiscoveredDevice(final BaseDevice baseDevice) {
        TS800 ts800 = (TS800)baseDevice;
    }

    @Override
    public void didScanStop() {
        //TODO scanning operation stop
    }
 }
```

### Connect TS800 Device 

Before operating remote TS800, please make sure that the remote TS800 is connected.
Please call connect method to connect a remote ts800.
If a connection is done, the didUpdateConnection(ConnectionState.CONNECTED,CommunicationType.BLE) callback method of CommunicationCallback will called.

To monitor the connection state with remote TS800, please set CommunicationCallback.
A connection callback function will called if the connection status with remote TS800 is changed.

```java
 CommunicationCallback mCommunicationCallback = new CommunicationCallback{
    @Override 
    public void didUpdateConnection(ConnectionState connectedState, CommunicationType type) {
        //Please call initializeSettings method every time when connection is done.
        if(commectedState==CONNECTED){
            ts800.initializeSettings();
        }
        //TODO There are three connectionState: Connected, Connecting and Disconnection. Please make sure the remote TS800 is connected before operating remote TS800.  
        //TODO TS800 has BLE, TCP connection types
    }
 }
 ts800.setCommunicationCallback(mCommunicationCallback);

```

Connect to the remote TS800.

```java
 ts800.connect();
```

To disconnect to remote TS800, please make sure every command is done.
If the TS800 is still inventorying Tags, please call stopInventory method.
After the `didGeneralSuccess("STOP_INVENTORY")` method is called, a disconnect method is able to called.

```java
 ts800.disconnect();
```

A connection callback function will called after the remote TS800 is disconnected.

### TS800 Operation

Before operating TS800, please set UHFCallback and connect to the remote TS800.

```java
UHFCallback uhfCallback = new UHFCallback{
    public void didGeneralSuccess(String invokeApi){
        //TODO receive an ack reply from remote TS800 reader
    };
    
    public void didGeneralError(String invokeApi, String errorMessage){
        //TODO receive an error reply from remote TS800 reader
    };
    
    public void didDiscoverTagInfo(TagInformationFormat tagInformationFormat){
        //TODO receive a tag event from remote TS800 reader
        //The callback method will called when the remote TS800 is inventory and a Epc Tag is found.
    };
    
    public void didReadEpc(byte[] epc){
        //TODO receive reply of readEpc method
        //The callback method of ts800.readEpc
    };

    public void didGetFirmwareVersion(String firmwareVersion){
        //TODO receive reply of getRomVersion method
        //The callback method of ts800.getRomVersion
    };
    
    public void didGetRfPower(byte rfPower){
        //TODO receive reply of getRfPower method
        //The callback method of ts800.getRfPower
    };
    
    public void didGetRfSensitivity(RfSensitivityLevel rfSensitivity){
        //TODO receive reply of getRfSensitivity method
        //The callback method of ts800.getRfSensitivity
    };
    
    public void didGetFrequencyList(ArrayList<Double> frequencyList){
        //TODO receive reply of getFrequency method
        //The callback method of ts800.getFrequency
    }

    public void didGetTriggerType(TriggerType triggerSource){
        //TODO receive reply of getTriggerType method
        //The callback method of ts800.getTriggerType
    }
    
    public void didGetSessionAndTarget(Session session, Target target){
        //TODO receive reply of getSessionAndTarget method
        //The callback method of ts800.getSessionAndTarget
    }

    public void didGetQValue(byte qValue){
        //TODO receive reply of getQValue method
        //The callback method of ts800.getQValue
    }

    //This Callback is only for TS100 remote device, TS800 is not supported
    public void didGetBuzzerOperationMode(BuzzerOperationMode buzzerOperationMode){
    }
}
ts800.setUHFCallback(uhfCallback);
```

#### Start Inventory

Using StartInventory method to start the process of an inventory round.
Some devices (TS800) may need call SetTriggerType method to indicate the way of triggering the inventory round.
You can get the inventory tag data (EPC/TID) from the OnTagPresented event.
Below sample code shows using StartInventory method and indicates the return data format is PC+EPC+TID.

```java
 ts800.startInventory(TagPresentedType.PC_EPC_TID);
```

After operating TS800, methods of UHFCallback function will called. 

Method `didGeneralSuccess("START_INVENTORY")` of UHFCallback will called if this operation is success.

Method `didGeneralError("START_INVENTORY",errorMessage)` of UHFCallback will called if this operation is failed.

Method `didDiscoverTagInfo` of UHFCallback will called if `startInvetory` operatino is success and a tag is found by TS800.

#### Stop Inventory

Using stopInventory method to stop the process of inventory round.

```java
 ts800.stopInventory();
```
After operating TS800, a method of UHFCallback function will called. 

Method `didGeneralSuccess("STOP_INVENTORY")` of UHFCallback will called if this operation is success.

Method `didGeneralError("STOP_INVENTORY",errorMessage)` of UHFCallback will called if this operation is failed.

#### Adjust Rf Power

Increase the RF output power may increase the reading tag distance. Using SetRfPower method to adjust the RF output power.
Below sample code shows using SetRfPower method to adjust the RF output stregth (19 dbm).

Set Rf Power to EEPROM(This setting will become effective immediately)

```java
 ts800.setRfPower(false, 19));
```

Set Rf Power setting for RAM(Current operating status)
```java
 ts800.setRfPower(true, 19));
```
After operating TS800, a method of UHFCallback function will called. 

Method `didGeneralSuccess("SET_RF_POWER")` of UHFCallback will called if this operation is success.

Method `didGeneralError("SET_RF_POWER",errorMessage)` of UHFCallback will called if this operation is failed.

#### Get Rf Power

Get Rf Power setting for EEPROM

```java
 ts800.getRfPower(false);
```

Get Rf Power setting for RAM(Current operating status)

```java
 ts800.getRfPower(true);
```
After operating TS800, a method of UHFCallback function will called. 

Method `didGetRfPower` of UHFCallback will called if this operation is success.

Method `didGeneralError("GET_RF_POWER",errorMessage)` of UHFCallback will called if this operation is failed.

Support
-------
[Related download](http://ftp.gigatms.com.tw/public/disks/disk5472/index.html)

License
-------

Unless required by applicable law or agreed to in writing, 
software distributed under the License is distributed on an "AS IS" BASIS, 
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
See the License for the specific language governing permissions and limitations under the 
[License](ftp://vip:26954214@ftp.gigatms.com.tw/public/disks/common/Documents/SOFTWARE_DEMOSTRATION_LICENSE_TM970228.pdf).

