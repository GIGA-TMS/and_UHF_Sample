# V 2.0.2 (SDK V2.2.0)
Influence Products: TS100, TS800, UR0250, PWD100, MU400H, NR800

## TS100, TS100A-00, Main: ROM-T1894 V1.09r5(1911260), BLE: ROM-T1857 V1.02R4   

### Scan BLE Device Page
#### Fix
* Can't find BLE device in Android 10: Android 10 need ACCESS_FINE_LOCATION permission to use bluetooth functions

### Read/Write Page
#### ADD
* Read Tag Ex with access password
* Read Tag Ex without access password
* Write Tag Ex with access password
* Write Tag Ex without access password

### Setting Page

#### ADD
* Get/Set RxDecode
* Get/Set LinkFrequency

### Advance Page
#### ADD
* Get/Set RemoteHost
* Get/Set Barcode Read Format
* GetWiFiMacAddress

#### FIX
* Get/Set Post Data Delimiter: CARRIAGE -> CR
* Get/Set Post Data Delimiter: LINE -> LF 
* Get/Set Post Data Delimiter: TAD -> TAB 


## TS800, TS800-00 Main: ROM-T1888 V1.00R1(1911240), BLE: ROM-1857 V1.02R7

### Scan BLE Device Page
#### Fix
* Can't find BLE device in Android 10: Android 10 need ACCESS_FINE_LOCATION permission to use bluetooth functions

### Setting Page
#### ADD
* Get/Set RxDecode
* Get/Set LinkFrequency

### Advance Page
#### ADD
* Get/Set RemoteHost
* GetWiFiMacAddress

## UR0250, UR0250-00 ROM-T1800 V1.00R0(1907310), BLE: ROM-T1857 V1.02R7

### Scan BLE Device Page
#### Fix
* Can't find BLE device in Android 10: Android 10 need ACCESS_FINE_LOCATION permission to use bluetooth functions

### Setting Page
#### ADD
* Get/Set RxDecode
* Get/Set LinkFrequency

### Advance Page
#### ADD
* Get/Set RemoteHost
* GetWiFiMacAddress

## MU400H, MU400H-00 ROM-T1870 V1.00r8(1912310)

### Read/Write Page
#### ADD
* Read Tag Ex with access password
* Read Tag Ex without access password
* Write Tag Ex with access password
* Write Tag Ex without access password

### Setting Page
#### ADD
* Get/Set RxDecode
* Get/Set LinkFrequency

### Advance Page
#### FIX
* Get/Set Post Data Delimiter: CARRIAGE -> CR
* Get/Set Post Data Delimiter: LINE -> LF 
* Get/Set Post Data Delimiter: TAD -> TAB 

## NR800, UR800-00 ROM-T1906 V1.00R0(1906121)

### Setting Page
#### ADD
* Get/Set RxDecode
* Get/Set LinkFrequency

### Advance Page
#### Adjust
* Show <CR> while "Enter Key" is typed in Get/Set Prefix, Suffix and TID Delimiter Block 

## PWD100, FW not release yet!

### Scan BLE Device Page
#### Fix
* Can't find BLE device in Android 10: Android 10 need ACCESS_FINE_LOCATION permission to use bluetooth functions

### Setting Page
#### ADD
* Get/Set RxDecode
* Get/Set LinkFrequency

### Advance Page
#### ADD
* Get/Set RemoteHost
* GetWiFiMacAddress
