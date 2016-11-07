/**
 *  Hue Scene
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
	definition (name: "Hue Scene", namespace: "penner42", author: "Alan Penner") {
		capability "Actuator"
        capability "Switch"
        capability "Momentary"
        capability "Sensor"
        
        command "updateScene"
	}

	simulator {
		// TODO: define status and reply messages here
	}

	tiles (scale: 2) {
	    multiAttributeTile(name:"switch", type: "generic", width: 6, height: 4, canChangeIcon: true){
			tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
				attributeState "on",  label:'Push', action:"momentary.push", icon:"st.lights.philips.hue-multi", backgroundColor:"#79b821"
			}
		}
	    standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state "default", label:"", action:"refresh.refresh", icon:"st.secondary.refresh"
		}
		standardTile("updateScene", "device.updateScene", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
    	   	state "Ready", label: 'UpdateScene', action:"updateScene", backgroundColor:"#F3C200"
	    }
	}
    
    main "switch"
    details(["switch","updateScene","sceneID","refresh"])
}

// parse events into attributes
def parse(String description) {
	log.debug "Parsing '${description}'"
	
}

/** 
 * capability.switch
 **/
def on() {
	push()
}

def off() {

}

/**
 * capablity.momentary
 **/
def push() {
	log.debug("Turning on!")
    
	sendEvent(name: "switch", value: "on", isStateChange: true, display: false)
	sendEvent(name: "switch", value: "off", isStateChange: true, display: false)
	sendEvent(name: "momentary", value: "pushed", isStateChange: true)    
    
    def commandData = parent.getCommandData(this.device.deviceNetworkId)
	log.debug(commandData)
    parent.sendHubCommand(new physicalgraph.device.HubAction(
    	[
        	method: "PUT",
			path: "/api/${commandData.username}/groups/0/action",
	        headers: [
	        	host: "${commandData.ip}"
			],
	        body: [scene: "${commandData.deviceId}"]
		])
	)
}

def updateScene() {
	log.debug("Updating scene!")
    def commandData = parent.getCommandData(this.device.deviceNetworkId)
	log.debug(commandData)
    parent.sendHubCommand(new physicalgraph.device.HubAction(
    	[
        	method: "PUT",
			path: "/api/${commandData.username}/scenes/${commandData.deviceId}/",
	        headers: [
	        	host: "${commandData.ip}"
			],
	        body: [storelightstate: true]
		])
	)	
}