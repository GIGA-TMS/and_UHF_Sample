# 2.1.0 
## Refactor 
#### TS100, TS800, UR0250, MU400H, PWD100, NR800
* setEventType(boolean temporary, EventType eventType) -> setEventType(boolean temporary, BaseTagEvent baseTagEvent)

## ADD
#### TS100A is now supported

## Fix
#### ReadTag with wrong result while the Bank is locked.

# 2.0.0.10 (Internal Testing)
## Refactor
#### TS100 & MU400H
* setPostDataDelimiter(boolean temporary, PostDataDelimiter postDataDelimiter) -> setPostDataDelimiter(boolean temporary, Set<PostDataDelimiter> postDataDelimiter)
* setMemoryBankSelection(boolean temporary, MemoryBankSelection memoryBankSelection) -> setMemoryBankSelection(boolean temporary, Set<MemoryBankSelection> memoryBankSelections)

#### UHFDevice
* writeEpc(String hexStringAccessPassword, String hexSelectedEpc, byte[] epcData) -> writeEpc(String hexSelectedPcEpc, String hexAccessPassword,  byte[] epcData)
* readTag(String hexStringPassword, String selectedPcEpc, MemoryBank memoryBank, int startWordAddress, int readLength) -> readTag(String hexSelectedPcEpc, String hexAccessPassword, MemoryBank memoryBank, int startWordAddress, int readLength)
* writeTag(String hexStringPassword, String selectedPcEpc, MemoryBank memoryBank, int startWordAddress, byte[] data) -> writeTag(String hexSelectedPcEpc, String hexAccessPassword, MemoryBank memoryBank, int startWordAddress, byte[] data)

## Add
#### TS100 & MU400H
* getInventoryActiveMode
* setInventoryActiveMode

# 2.0.0.9
## Fix
#### NR800
* Null value while setting suffix, prefix, and tid delimiter.

# 2.0.0.8
* Add Error Handle of UHFDevice, TS100, TS800, UR0250, MU400H, NR800
* Refactor function name of MU400H.setOutputInterface -> MU400H.setOutputInterface  

# 2.0.0.7
## Refactor
#### UHFDevice
* getRomVersion -> getFirmwareVersion 
## Add
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
## Remove
#### Product Supported Trigger Feature
* trigger init in initializeSetting function

# 2.0.0.6
## Add
#### Test and ASCII Adapter

# 2.0.0.5
## Add
#### NR800
* setTextTagEventType
* getTextTagEventType
# 2.0.0.4
## Add
* Gradle: Proguard 

# 2.0.0.3
## Refactor
#### BLE relative method
* getBleDeviceName of UHFDevice -> getBleDeviceName of UR0250, TS800, TS100
* setBleDeviceName of UHFDevice -> getBleDeviceName of UR0250, TS800, TS100
* getBleRomVersion of UHFDevice -> getBleRomVersion of UR0250, TS800, TS100
#### UHFDevice
* setTagRemovedEventThreshold -> setTagRemovedThreshold
* getTagRemovedEventThreshold -> getTagRemovedThreshold
* setTagPresentedEventThreshold -> setTagPresentedRepeatInterval
* getTagPresentedEventThreshold -> getTagPresentedRepeatInterval

## Add
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

# 2.0.0.2
### Refactor
#### Common Function:
* getDeviceMacAddr and getDeviceIp -> getDeviceID

#### TS100
* getOutputInterface -> getOutputInterfaces
* setOutputInterface -> setOutputInterfaces
* setMemoryBankSelection(boolean temporary, Set<MemoryBankSelection> memoryBankSelections) -> setMemoryBankSelection(boolean temporary, MemoryBankSelection memoryBankSelection)
* enableFilter and disableFilter -> setFilter



## Add
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

# 2.0.0.1

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
