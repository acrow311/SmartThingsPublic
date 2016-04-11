/**
 *  Stereo Control
 *
 *  Copyright 2016 Andrew Crow
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
definition(
    name: "Master/Slave Switch Control",
    namespace: "acrow311",
    author: "Andrew Crow",
    description: "Control Slave Smart Plug based on Master Smart Plug on/off or power consumption change (turning device off or on).",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/App-BigButtonsAndSwitches.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/App-BigButtonsAndSwitches@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/App-BigButtonsAndSwitches@2x.png")


preferences {
	section("Select Smartpower Switches") {
		input(name: "MasterSwitch", type: "capability.switch", title: "Controlling Device (Master)", required: true, multiple: false, description: null)
        input(name: "threshold", type: "number", title: "Power Control Level", required: true, description: "In watts, enter integer value")
        input(name: "SlaveSwitch", type: "capability.switch", title: "Switch to be Controlled (Slave)", required: true, multiple: false, description: null)
	}
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	initialize()
}

def initialize() {
	unsubscribe()
	subscribe(MasterSwitch, "power", meterHandler)
    subscribe(MasterSwitch, "switch", switchHandler)
}

def meterHandler(evt) {
    def meterValue = evt.value as double
    def thresholdValue = threshold as int
	def masterSwitchState = MasterSwitch.currentValue("switch") == "on"  // Get current master switch status (on = true)
    def slaveSwitchState = SlaveSwitch.currentValue("switch") == "on" // Get current slave switch status (on = true)
	
	if (slaveSwitchState) { // When the slave is on
    
    	if (masterSwitchState) { // And the master is on
        	if (meterValue < thresholdValue) { // If the power consumption is low enough, turn off switch
                log.info "${MasterSwitch} reported energy ${meterValue} below ${threshold}. Turning off ${SlaveSwitch}."
                sendNotificationEvent("${MasterSwitch} reported energy ${meterValue} below ${threshold}. Turning off ${SlaveSwitch}.")
                SlaveSwitch.off()
            } 
        } else { // And Master is off - turn off (this will probably never fire as the switch off event should fire instead)
        	log.info "${MasterSwitch} turned off. Turning off ${SlaveSwitch} (Meter Event)."
            sendNotificationEvent("${MasterSwitch} turned off. Turning off ${SlaveSwitch}.")
            SlaveSwitch.off()
          }
          
    } else { // When slave is off
    
        if (masterSwitchState) { // And the master is on
            if (meterValue > thresholdValue) { // If master power consumption is high enough, turn on slave
                log.info "${MasterSwitch} reported energy ${meterValue} above ${threshold}. Turning on ${SlaveSwitch}."
                sendNotificationEvent("${MasterSwitch} reported energy ${meterValue} above ${threshold}. Turning on ${SlaveSwitch}.")
                SlaveSwitch.on()
            } 
        } 
    }
}

def switchHandler(evt) {
	def masterSwitchState = MasterSwitch.currentValue("switch") == "on"  // Get current master switch status (on = true)
    def slaveSwitchState = SlaveSwitch.currentValue("switch") == "on" // Get current slave switch status (on = true)
    
    if (masterSwitchState) { // The master was switched on, do nothing
    } else { // The master was switched off
    	log.info "${MasterSwitch} just turned off. Turning off ${SlaveSwitch}."
        sendNotificationEvent("${MasterSwitch} just turned off. Turning off ${SlaveSwitch}.")
    	SlaveSwitch.off()
    }
}