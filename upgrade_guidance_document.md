# TS800, TS100 and UR0250 SDK upgrade to UHF SDK Instructions for Android

Introduction
------------
This documents show the instruction for merging to UHF SDK V2.0.0.0.

SDK version:
- TS800 V2.0.0.15
- TS100 V2.0.0.6
- UR0250 V2.0.0.2

Instructions
------------

### UHFCallback
UHDCallback is now a class now a interface.

If current code is like:
```java
public class DeviceControlFragment extends Fragment implements UHFCallback{
    ......
    @Override
    public void onResume() {
        super.onResume();
        mUhfDevice.setUHFCallback(this);
    }

    @Override
    public void didGeneralSuccess(String invokeApi) {
        
    }

    @Override
    public void didGeneralError(String invokeApi, String errorMessage) {
        
    }
    ......
}
```
Please modify to:
```java
public class DeviceControlFragment extends Fragment{
    ......
    @Override
    public void onResume() {
        super.onResume();
        mUhfDevice.setUHFCallback(mUHFCallback);
    }

    UHFCallback mUHFCallback = new UHFCallback() {
        @Override
        public void didGeneralSuccess(String invokeApi) {
           
        }

        @Override
        public void didGeneralError(String invokeApi, String errorMessage) {
           
        }
        ......
    }
    ......
}
```

### TS800 Constructors
#### Communicating with TS800 through Wi-Fi
If using the constcutor as below,

```java
    TS800 mTS800 = new TS800(deviceName, wifiMacAddress, ip, port);
```

Please motify to the code as below.

```java
    BaseDeviceInfo baseDeviceInfo = new BaseDeviceInfo.Builder()
            .setTcpTransceiverInfo(deviceName,wifiMacAddress,ip,port)
            .build();
    TS800 mTS800 = new TS800(baseDeviceInfo);
```

#### Communicating with TS800 through BLE
If using the constcutor as below,
```java
    TS800 mTS800 = new  TS800(deviceName, bleMacAddress, context);
```
Please motify to the code as below.
```java
    BaseDeviceInfo baseDeviceInfo = new BaseDeviceInfo.Builder()
        .setBleTransceiverInfo(deviceName,bleMacAddress)
        .build();
    TS800 mTS800 = new TS800(context,baseDeviceInfo);
```

### UR0250 Constructor
#### Communicating with UR0250 through Wi-Fi
If using the constcutor as below:

```java
    UR0250 mUR0250 = new UR0250(deviceName, wifiMacAddress, ip, port);
```

Please motify to the code as below:

```java
    BaseDeviceInfo baseDeviceInfo = new BaseDeviceInfo.Builder()
            .setTcpTransceiverInfo(deviceName,wifiMacAddress,ip,port)
            .build();
    UR0250 mUR0250 = new UR0250(baseDeviceInfo);
```

#### Communicating with UR0250 through BLE
If using the constcutor as below:
```java
    UR0250 mUR0250 = new  UR0250(deviceName, bleMacAddress, context);
```
Please motify to the code as below:
```java
    BaseDeviceInfo baseDeviceInfo = new BaseDeviceInfo.Builder()
        .setBleTransceiverInfo(deviceName,bleMacAddress)
        .build();
    UR0250 mUR0250 = new UR0250(context,baseDeviceInfo);
```
### TS100 Constructor
#### Communicating with TS100 through Wi-Fi
If using the constcutor as below:

```java
    TS100 mTS100 = new TS100(deviceName, wifiMacAddress, ip, port);
```

Please motify to the code as below:

```java
    BaseDeviceInfo baseDeviceInfo = new BaseDeviceInfo.Builder()
            .setTcpTransceiverInfo(deviceName,wifiMacAddress,ip,port)
            .build();
    TS100 mTS100 = new TS100(baseDeviceInfo);
```

#### Communicating with TS100 through BLE
If using the constcutor as below:
```java
    TS100 mTS100 = new  TS100(deviceName, bleMacAddress, context);
```
Please motify to the code as below:
```java
    BaseDeviceInfo baseDeviceInfo = new BaseDeviceInfo.Builder()
        .setBleTransceiverInfo(deviceName,bleMacAddress)
        .build();
    TS100 mTS100 = new TS100(context,baseDeviceInfo);
```
### CommunicationCallback Interface
CommunicationCallback interface has add `didConnectionTimeout` function.
If the CommunicationCallback is implemented as below:
```java
public class DeviceControlFragment extends Fragment implements CommunicationCallback{
    ......
    @Override
    public void didUpdateConnection(ConnectionState connectState, CommunicationType type) {
        ......
    }
    ......
}
```
Please motify to the code as below:
```java
public class DeviceControlFragment extends Fragment implements CommunicationCallback{
    ......
    @Override
    public void didUpdateConnection(ConnectionState connectState, CommunicationType type) {
        ......
    }

    @Override
    public void didConnectionTimeout(CommunicationType type) {
        ......
    }
    ......
}
```

### TagInformationFormat

The methods of TagInfomationFormat as below are deleted.
```java
    public int getScanCount();
    public int setScanCountPlus();
    public int getTagNumber();
    public int getEPCLength();
    public int getTIDLength();
```
The methods of TagInfomationFormat are refactored as below .
```java
    public String getEPCHexString() -> public String getPcEPCHex();
    public byte[] getFrequency() ->    public double getFrequency();
```

### BaseDevice Constructor

Two constructors of BaseDevice as below are deleted.
```java
    public BaseDevice(String deviceName, String deviceRomVersion, String macAddress, String ip, String port);

    public BaseDevice(String deviceName, String deviceRomVersion
            , String macAddress, Context context
            , UUID service, UUID characteristicWrite, UUID characteristicNotify, UUID descriptorNotify);
```
Please use TS800, TS100 or UR0250 classes while trying to new a BaseDevice.
## UHFDevice
### UHFDevice Constructor

Two constructors of BaseDevice as below are deleted.
```java
    public UHFDevice(String deviceName, String wifiMacAddress, String ip, String port);

    public UHFDevice(String deviceName, String wifiMacAddress, String ip, String port);
```
Please use TS800, TS100 or UR0250 classes while trying to new a UHFDevice.

--------
# Upgrade UHF SDK from 2.0.0.1 to 2.0.0.2

## BaseDevice 

getDeviceMacAddr and getDeviceIp method is now deleted.

```java
    public String getDeviceMacAddr() 

    public String getDeviceIp()
```
Please call `getDeviceID` method to get BLE Mac Address while connected with remote device through BLE.

Please call `getDeviceID` method to get Ip while connected with remote device through TCP(Wi-Fi).

It is not able to get the macAddres of remote device while connected through TCP(Wi-Fi)

--------
# Upgrade UHF SDK from 2.0.0.2 to 2.0.0.3

## Get and Set BLE Device Name

The method of get and set BLE device name is now not in UHFDevice. Please set or get BLE device name as below.

```java
getBleDeviceName of TS800, TS100, UR0250
setBleDeviceName of TS800, TS100, UR0250
```

## Get BLE ROM Version

The method of get and set BLE device name is now not in UHFDevice. Please set or get BLE ROM version as below.

```java
getBleRomVersion of TS800, TS100, UR0250
```

## UHFDevice

The methods below are refactor.

```java
setTagRemovedEventThreshold -> setTagRemovedThreshold
getTagRemovedEventThreshold -> getTagRemovedThreshold
setTagPresentedEventThreshold -> setTagPresentedRepeatInterval
getTagPresentedEventThreshold -> getTagPresentedRepeatInterval
```

## UHFCallback

The methods below are refactor.
```java
didGetTagPresentedEventThreshold -> didGetTagPresentedRepeatInterval
didGetTagRemovedEventThreshold -> didGetTagRemovedThreshold
```
--------
# Upgrade UHF SDK from 2.0.0.6 to 2.0.0.7

## UHFDevice

The methods below are refactor.

```java

setFrequency(final boolean temporary, final ArrayList<Double> frequencyList) -> setFrequency(final boolean temporary, final Set<Double> frequencyList)

getRomVersion() -> getFirmwareVersion()
```

## TS800, UR0250

The methods below are refactor.

```java
setTriggerType(boolean temporary, TriggerType triggerType) ->  setTriggerType(boolean temporary, Set<TriggerType> triggerType)
```


## UHFCallback

The methods below are refactor.
```java
didGetFrequencyList(List<Double> frequencyList) -> didGetFrequencyList(Set<Double> frequencyList) 
didGetTriggerType(Set<TriggerType> triggerSources) -> didGetTriggerType(TriggerType triggerSource) 
```

## Device Supported Trigger 

Please read readme.md document to implement device with trigger.

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

