import groovy.json.JsonSlurper
import java.text.DateFormat

metadata {
	definition (name: "xiaomi weather", namespace: "fison67", author: "fison67", vid: "SmartThings-smartthings-Xiaomi_Temperature_Humidity_Sensor", ocfDeviceType: "oic.d.thermostat") {
        capability "Temperature Measurement"
        capability "Relative Humidity Measurement"
        capability "Sensor"
        capability "Battery"
        capability "Refresh"
         
        attribute "pressure", "string"
	}

	simulator {}
    
	preferences {
		input name: "temperatureType", title:"Select a type" , type: "enum", required: true, options: ["C", "F"], defaultValue: "C"
	}
}

// parse events into attributes
def parse(String description) {
	log.debug "Parsing '${description}'"
}

def setInfo(String app_url, String id) {
	log.debug "${app_url}, ${id}"
	state.app_url = app_url
    state.id = id
}

def setExternalAddress(address){
	log.debug "External Address >> ${address}"
	state.externalAddress = address
}

def setStatus(params){
    log.debug "${params.key} : ${params.data}"
    
 	switch(params.key){
    case "relativeHumidity":
		def para = "${params.data}"
		String data = para
		def stf = Float.parseFloat(data)
		def humidity = Math.round(stf)
    	sendEvent(name:"humidity", value: humidity, unit: "%" )
    	break;
    case "temperature":
		def para = "${params.data}"
		String data = para
		def st = data.replace("C","");
		def stf = Float.parseFloat(st)
		def tem = Math.round(stf*10)/10
        def temperatue = makeTemperature(tem)
        sendEvent(name:"temperature", value: temperatue, unit: temperatureType == null ? "C" : temperatureType)
    	break;
    case "atmosphericPressure":
    	sendEvent(name:"pressure", value: params.data.replace(" Pa","").replace(",","").toInteger()/1000 )
    	break;
    case "batteryLevel":
    	sendEvent(name:"battery", value: params.data, unit:"%" )
    	break;	
    }
}

def makeTemperature(temperature){
	if(temperatureType == "F"){
    	return ((temperature * 9 / 5) + 32)
    }else{
    	return temperature
    }
}

def updated() {}

def refresh(){
	log.debug "Refresh"
    def options = [
     	"method": "GET",
        "path": "/devices/get/${state.id}",
        "headers": [
        	"HOST": parent._getServerURL(),
            "Content-Type": "application/json"
        ]
    ]
    sendCommand(options, callback)
}

def sendCommand(options, _callback){
	def myhubAction = new physicalgraph.device.HubAction(options, null, [callback: _callback])
    sendHubCommand(myhubAction)
}

def callback(physicalgraph.device.HubResponse hubResponse){
	def msg
    try {
        msg = parseLanMessage(hubResponse.description)
		def jsonObj = new JsonSlurper().parseText(msg.body)
        log.debug jsonObj
        
 		sendEvent(name:"battery", value: jsonObj.properties.batteryLevel)
        sendEvent(name:"temperature", value: makeTemperature(jsonObj.properties.temperature.value))
        sendEvent(name:"humidity", value: jsonObj.properties.relativeHumidity)
        sendEvent(name:"pressure", value: jsonObj.properties.atmosphericPressure.value / 1000)
        
        updateLastTime()
        checkNewDay()
    } catch (e) {
        log.error "Exception caught while parsing data: "+e;
    }
}