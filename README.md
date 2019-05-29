# RaceTraq
## Alex Newman and Leina McDermott, CS65 Spring 2019

### Introduction

RaceTraq is an app designed specifically for the needs of the Dartmouth Formula Racing 
team. The team's car is controlled by a "Central Vehicle Controller" that, among other
things, logs data from the car's numerous sensors. The data is currently logged to an SD
card, however, the team would like to have a way of storing the data in the cloud and of
seeing the logged data in real-time from a remote device.

RaceTraq is our answer to this need. The app uses the built-in Bluetooth module on the 
Central Vehicle Controller to receive logged data from the controller. It then immediately
posts the data to a Firebase Realtime Database, where the data is easily accessible 
remotely. In addition to posting Dartmouth Formula Racing's data to the cloud, the app 
also supplements GPS data from the host device in order to provide maps of Dartmouth
Formula Racing's logged drives.

### Instructions

1. 	Open the RaceTraq app on an Android device.
2. 	Enter a previously-registered e-mail and password and hit the `Sign In` button or
	enter a new email and password and hit the `Register` button.
3. 	Click on the Bluetooth button to try to connect to a Central Vehicle Controller. Grant
	the app location access to proceed.
4. 	Ensure device is within Bluetooth range of a Central Vehicle Controller by checking
	the list of devices for the `Adafruit Bluefruit` device. If the device does not 
	appear, try refreshing the list by pulling down on the list. Once the device has been
	located, click its `Connect` button.
5. 	Ensure that a `Device Connected` alert is received and click `OK`. If the connection
	attempt fails instead, check that the Central Vehicle Controller is still on and 
	repeat step 3.
6. 	Once connected, observe the new status displayed at the top of the MainActivity. When
	ready to start a new data-logging session, click on the `NEW DRIVE` button.
7. 	Switch between `Dash`, `Map` and `Plot` views using the three tabs at the 
	bottom of the screen. When the drive is finished, click the `END DRIVE` button.
8. 	Enter a name for the drive in the dialog window and click the `SAVE` button. 
9. 	From the main menu, use the drawer to navigate to the `Saved Drives` tab and click on
	it to see a list of the saved drives.
10. To see more details on a drive, click on the drive of interest. To delete the drive,
	click on the `DELETE` button.
11. From the main menu, use the Bluetooth icon to disconnect Bluetooth or use the drawer 
	to logout of the app.