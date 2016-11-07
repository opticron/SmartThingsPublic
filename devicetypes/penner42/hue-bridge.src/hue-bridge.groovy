/**
 *  Hue Bridge
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
	definition (name: "Hue Bridge", namespace: "penner42", author: "Alan Penner") {
		capability "Actuator"

		attribute "serialNumber", "string"
		attribute "networkAddress", "string"
		attribute "username", "string"

		command "discoverItems"
	}

	simulator {
		// TODO: define status and reply messages here
	}

	tiles(scale: 2) {
        standardTile("bridge", "device.username", width: 6, height: 4) {
        	state "default", label:"Hue Bridge", inactivelabel:true, icon:"st.Lighting.light99-hue", backgroundColor: "#F3C200"
        }
		main "bridge"
		details "bridge"
	}
}

def discoverItems() {
	log.debug("Bridge discovering items.")
	def host = this.device.currentValue("networkAddress") + ":80"
	def username = this.device.currentValue("username")

	def result = new physicalgraph.device.HubAction(
			method: "GET",
			path: "/api/${username}/",
			headers: [
					HOST: host
			]
	)
	return result
}

def handleParse(desc) {
log.debug("handle")
	parse(desc)
}

// parse events into attributes
def parse(String description) {
	def parsedEvent = parseLanMessage(description)
	if (parsedEvent.headers && parsedEvent.body) {
		def headerString = parsedEvent.headers.toString()
		if (headerString.contains("application/json")) {
			def body = new groovy.json.JsonSlurper().parseText(parsedEvent.body)
			def bridge = parent.getBridge(parsedEvent.mac)
			/* response from bulb/group/scene command. Figure out which device it is, then pass it along to the device. */
			if (body[0] != null && body[0].success != null) {
				body.each{
					it.success.each { k, v ->
						def spl = k.split("/")
						def devId = ""
						if (spl[4] == "scene") {
							devId = bridge.value.mac + "/SCENE" + v
						} else {
							if (spl[1] == "lights") {
								spl[1] = "BULBS"
							}
							devId = bridge.value.mac + "/" + spl[1].toUpperCase()[0..-2] + spl[2]
						}
						def d = parent.getChildDevice(devId)
						log.debug(d)
						log.debug ("${devId} ${spl[3]} ${spl[4]} ${v}")
						d.updateStatus(spl[3], spl[4], v)
					}
				}
			} else if (body[0] != null && body[0].error != null) {
				log.debug("Error: ${body}")
			} else if (bridge) {
				def bulbs = [:] //bridge.value.bulbs
				def groups = [:] //bridge.value.groups
				def scenes = [:] //bridge.value.scenes

				body.lights?.each { k, v ->
					bulbs[k] = [id: k, name: v.name, type: v.type, state: v.state]
			    }
                body.groups?.each { k, v -> 
                    groups[k] = [id: k, name: v.name, type: v.type, action: v.action]
				}
                body.scenes?.each { k, v -> 
                    scenes[k] = [id: k, name: v.name]
				}
				return createEvent(name: "itemDiscovery", value: device.hub.id, isStateChange: true, data: [bulbs, scenes, groups, bridge.value.mac])
			}
		} else {
			log.debug("Unrecognized messsage: ${parsedEvent.body}")
		}
	}
	return []
}