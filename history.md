# SDK History
## 2.0.0.7 (2019/10/30)
### Refactor
#### UHFDevice
* getRomVersion -> getFirmwareVersion 
### Add
#### Product Supported Trigger Feature
* setScanMode
* getScanMode
* setCommandTriggerState
* getCommandTriggerState
* setTriggerType
* getTriggerType
#### UHFDevice 
* getInventoryActiveMode
* setInventoryActiveMode
* lockTag
* killTag
### Remove
#### Product Supported Trigger Feature
* trigger init in initializeSetting function

## 2.0.0.6 (2019/10/07)
### Add
#### Text and ASCII Adapter

## 2.0.0.5 (2019/10/05)
### Add
#### NR800
* setTextTagEventType
* getTextTagEventType
## 2.0.0.4 (2019/10/05)
### Add
* Gradle: Proguard 

## 2.0.0.3 (2019/10/05)
### Refactor
#### BLE relative method
* getBleDeviceName of UHFDevice -> getBleDeviceName of UR0250, TS800, TS100
* setBleDeviceName of UHFDevice -> getBleDeviceName of UR0250, TS800, TS100
* getBleRomVersion of UHFDevice -> getBleRomVersion of UR0250, TS800, TS100
#### UHFDevice
* setTagRemovedEventThreshold -> setTagRemovedThreshold
* getTagRemovedEventThreshold -> getTagRemovedThreshold
* setTagPresentedEventThreshold -> setTagPresentedRepeatInterval
* getTagPresentedEventThreshold -> getTagPresentedRepeatInterval

### Add
#### Supported device
* NR800, PWD100
#### NR800
* setTagEventInterval
* getTagEventInterval
* getBuzzerOperationMode
* setBuzzerOperationMode
* controlBuzzer
* getVibratorState
* setVibratorState
* getPrefix
* getSuffix
* getTidDelimiter
* setScanMode
* getScanMode
#### PWD100
* setWifiSettings
* getInventoryOption
* newSearchingTagCondition
* appendSearchingTagCondition

## 2.0.0.2 (2019/09/20)
### Refactor
#### Common Function:
* getDeviceMacAddr and getDeviceIp -> getDeviceID

#### TS100
* getOutputInterface -> getOutputInterfaces
* setOutputInterface -> setOutputInterfaces
* setMemoryBankSelection(boolean temporary, Set<MemoryBankSelection> memoryBankSelections) -> setMemoryBankSelection(boolean temporary, MemoryBankSelection memoryBankSelection)
* enableFilter and disableFilter -> setFilter



### Add
#### Common Function:
* setTagRemovedEventThreshold
* getTagRemovedEventThreshold
* setTagPresentedEventThreshold
* getTagPresentedEventThreshold


#### TS800 Specific Function:
* getOutputInterface
* setOutputInterface
* getBuzzerOperationMode
* setBuzzerOperationMode
* controlBuzzer

#### TS100 Specific Function
* getFilter

## 2.0.0.1 (2019/07/30)

#### Supported Product:
* TS800
* UR0250
* TS100

#### Common Function:
* initializeSettings
* startInventory
* stopInventory
* readEpc
* writeEpc
* readTag
* readTag
* writeTag
* writeTag
* getBleDeviceName
* getRfPower
* setRfPower
* getRfSensitivity
* setRfSensitivity
* setFrequency
* getQValue
* setQValue
* getFrequency
* setSessionAndTarget
* getSessionAndTarget
* getRomVersion
* getBleRomVersion

#### TS800 Specific Function:
* setTriggerType
* getTriggerType
* getIOState
* setIOState

#### TS100 Specific Function:
* startInventoryEx
* getBuzzerOperationMode
* setBuzzerOperationMode
* enableFilter
* disableFilter
* getFilter
* controlBuzzer
* getEventType
* setEventType
* getOutputInterface
* setOutputInterface
* getPostDataDelimiter
* setPostDataDelimiter
* getMemoryBankSelection
* setMemoryBankSelection

#### UR0250 Specific Function:
* setTriggerType
* getTriggerType
* getIOState
* setIOState

--------------

# Sample Code History
* Refactor UI by Product
## 2.0.0.3-beta01 (2019/10/05)
* SDK 2.0.0.4
* Migrate to androidX
## 2.0.0.2 (2019/9/20)
* SDK v2.0.0.2
* Refactor Data of View Component: ViewCommand to ViewParamData
* Add Sub class of ViewParamData  
## 2.0.0.1 (2019/7/31)
* Refactor View Component for every Function of Device
* SDK v2.0.0.1
## 2.0.0.0 (2019/07/26)
* SDK v2.0.0.0