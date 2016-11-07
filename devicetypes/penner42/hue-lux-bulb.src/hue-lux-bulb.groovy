/**
 *  Hue Lux Bulb
 *
 *  Copyright 2016 Alan Penner
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
metadata {
	definition (name: "Hue Lux Bulb", namespace: "penner42", author: "Alan Penner") {
		capability "Switch Level"
		capability "Actuator"
		capability "Switch"
		capability "Refresh"
		capability "Polling"
		capability "Sensor"
        
		attribute "transitiontime", "number"
        
        command "updateStatus"
        command "flash"        
	    command "ttUp"
        command "ttDown"
	}

	simulator {
		// TODO: define status and reply messages here
	}

	tiles(scale: 2) {
        multiAttributeTile(name:"rich-control", type: "lighting", canChangeIcon: true){
            tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
              attributeState "on", label:'${name}', action:"switch.off", icon:"st.lights.philips.hue-single", backgroundColor:"#79b821", nextState:"turningOff"
              attributeState "off", label:'${name}', action:"switch.on", icon:"st.lights.philips.hue-single", backgroundColor:"#ffffff", nextState:"turningOn"
              attributeState "turningOn", label:'${name}', action:"switch.off", icon:"st.lights.philips.hue-single", backgroundColor:"#79b821", nextState:"turningOff"
              attributeState "turningOff", label:'${name}', action:"switch.on", icon:"st.lights.philips.hue-single", backgroundColor:"#ffffff", nextState:"turningOn"
            }
            tileAttribute ("device.level", key: "SLIDER_CONTROL") {
              attributeState "level", action:"switch level.setLevel", range:"(0..100)"
            }
        }

        standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "default", label:"", action:"refresh.refresh", icon:"st.secondary.refresh"
        }
		standardTile("flash", "device.flash", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state "default", label:"Flash", action:"flash", icon:"st.lights.philips.hue-single"
		}        
        /* transition time */
		valueTile("ttlabel", "transitiontime", decoration: "flat", width: 4, height: 1) {
			state "default", label:'Transition Time: ${currentValue}00ms'
		}
		valueTile("ttdown", "device.transitiontime", decoration: "flat", width: 1, height: 1) {
			state "default", label: "-", action:"ttDown"
		}
		valueTile("ttup", "device.transitiontime", decoration: "flat", width: 1, height: 1) {
			state "default", label:"+", action:"ttUp"
		}
        
        main(["rich-control"])
        details(["rich-control", "ttlabel","ttdown","ttup", "refresh", "flash"])
    }

}

// parse events into attributes
def parse(String description) {
	log.debug "Parsing '${description}'"

}

def ttUp() {
	def tt = this.device.currentValue("transitiontime")
    if (tt == null) { tt = 4 }
    log.debug "ttup ${tt}"
    sendEvent(name: "transitiontime", value: tt + 1)
}

def ttDown() {
	def tt = this.device.currentValue("transitiontime")
    if (tt == null) { tt = 4 }
    tt = tt - 1
    if (tt < 0) { tt = 0 }
    log.debug "ttdown ${tt}"
    sendEvent(name: "transitiontime", value: tt)
}

/** 
 * capability.switchLevel 
 **/
def setLevel(level) {
	def lvl = parent.scaleLevel(level, true)
	log.debug "Setting level to ${lvl}."
    
    def commandData = parent.getCommandData(device.deviceNetworkId)
    def tt = this.device.currentValue("transitiontime")
    if (tt == null) { tt = 4 }
    
    return new physicalgraph.device.HubAction(
    	[
        	method: "PUT",
			path: "/api/${commandData.username}/lights/${commandData.deviceId}/state",
	        headers: [
	        	host: "${commandData.ip}"
			],
	        body: [on:true, bri: lvl, transitiontime: tt]
		])
}

/** 
 * capability.switch
 **/
def on() {
	log.debug("Turning on!")
    
    def commandData = parent.getCommandData(device.deviceNetworkId)
    def tt = this.device.currentValue("transitiontime")
    if (tt == null) { tt = 4 }
    parent.sendHubCommand(new physicalgraph.device.HubAction(
    	[
        	method: "PUT",
			path: "/api/${commandData.username}/lights/${commandData.deviceId}/state",
	        headers: [
	        	host: "${commandData.ip}"
			],
	        body: [on: true, bri: 254, transitiontime: tt]
		])
	)
}

def off() {
	log.debug("Turning off!")
    
    def commandData = parent.getCommandData(device.deviceNetworkId)
    def tt = this.device.currentValue("transitiontime")
    if (tt == null) { tt = 4 }
    parent.sendHubCommand(new physicalgraph.device.HubAction(
    	[
        	method: "PUT",
			path: "/api/${commandData.username}/lights/${commandData.deviceId}/state",
	        headers: [
	        	host: "${commandData.ip}"
			],
	        body: [on: false, transitiontime: tt]
		])
	)
}



/** 
 * capability.polling
 **/
def poll() {
	refresh()
}

/**
 * capability.refresh
 **/
def refresh() {
	parent.doDeviceSync()
}

def flash() {
	log.debug "Flashing..."
    def commandData = parent.getCommandData(device.deviceNetworkId)
	parent.sendHubCommand(new physicalgraph.device.HubAction(
    	[
        	method: "PUT",
			path: "/api/${commandData.username}/lights/${commandData.deviceId}/state",
	        headers: [
	        	host: "${commandData.ip}"
			],
	        body: [alert: "lselect"]
		])
	)
    
    runIn(5, flash_off)
}

def flash_off() {
	log.debug "Flash off."
    def commandData = parent.getCommandData(device.deviceNetworkId)
	parent.sendHubCommand(new physicalgraph.device.HubAction(
    	[
        	method: "PUT",
			path: "/api/${commandData.username}/lights/${commandData.deviceId}/state",
	        headers: [
	        	host: "${commandData.ip}"
			],
	        body: [alert: "none"]
		])
	)
}

def updateStatus(action, param, val) {
	if (action == "state") {
		switch(param) {
        	case "on":
            	def onoff
            	if (val) { onoff = "on" } else { onoff = "off" }
            	sendEvent(name: "switch", value: onoff)
                break
            case "bri":
            	sendEvent(name: "level", value: parent.scaleLevel(val))
                break
			case "transitiontime":
            	sendEvent(name: "transitiontime", value: val)
                break
        }
    }
}