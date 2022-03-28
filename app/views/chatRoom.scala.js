@(username: String)
var attempts = 0;
var timeout;
var refreshIntervalId;
var chatSocket;
function createWebSocket() {
	$("#onChat").show()
	
	var WS = window['MozWebSocket'] ? MozWebSocket : WebSocket			
		    //chatSocket = new WS("ws://localhost.workapps.com:9000/rms/chat");		
	       // chatSocket = new WSS("@routes.ChatController.chat().webSocketURL(request)");		
			 chatSocket = new WS("wss://localhost.workapps.com/rms/chat");		
			var user;
			
			// Set timeout so that it gets time to make websocket connection. 
			// send ping message to server so that it won't close the connection
			timeout = setTimeout(function() {
				refreshIntervalId = setInterval(function() { 
					chatSocket.send(JSON.stringify({"type":"6"}));
				}, 10000);
			}, 10000);

			
			var onClose = function() {
				console.log('onclose');
				clearTimeout(timeout);
				clearInterval(refreshIntervalId);
				attempts++;
				setTimeout(function() {					
				console.log('attempts: '+attempts);
				console.log('connection is closed');
					createWebSocket();
				},5000);
			}
			var onError = function() {
				console.log('error');
				//clearTimeout(timeout);
				//clearInterval(refreshIntervalId);
				//attempts++;
				//setTimeout(function() {					
					//console.log('attempts: '+attempts);
					//console.log('connection is closed');
					//createWebSocket();
				//},5000);
			}
			var onOpen = function() {
				console.log('open');
			}

			var sendMessage = function() {
				var group = $("#group").val();
				var rec = $("#recipients").val();
				var text = $("#talk").val();
				var from = $("#from").val();
				var subType=0;//one2one
				if(group!=null && group!=""){
					rec=group;
					subType=1;
				}
				var msg = JSON.stringify({"text":text,"to":rec,"type":"1", "subtype":subType,
					"cid":"100"});
				console.log('input msg: '+msg)
				chatSocket.send(msg);
				$("#talk").val('')

				var da = new Object();
				da.type=4;
				da.subtype=0;
				da.action="UpdateChatStatusRead";
				da.params = new Object();
				da.params.to=rec;
				console.log('IQ request: '+JSON.stringify(da));

				chatSocket.send(JSON.stringify(da));
			}

			var receiveEvent = function(event) {
				var data = JSON.parse(event.data);
				if (data.type == 0) {
					user = data.user;
					console.log("user info "+ JSON.stringify(user));
				}
				else if (data.type == 1) {
					console.log("chat: "+ JSON.stringify(data));
				}
				else if (data.type == 2) {
					console.log("presence: "+ JSON.stringify(data));
				}
				else if (data.type == 3) {
					console.log("error: "+ JSON.stringify(data));
				}
				else if (data.type == 4) {
					console.log("iq: "+ JSON.stringify(data));
				}
				else if (data.type == 5) {
					console.log("ack: "+ JSON.stringify(data));
				}
				else if (data.type == 6) {
					console.log("ping: "+ JSON.stringify(data));
				}
				else if (data.type == 7) {
					console.log("event: "+ JSON.stringify(data));
				}
				else if (data.type == 8) {
					console.log("notification: "+ JSON.stringify(data));
				}
				else {
					console.log("other: "+ JSON.stringify(data));
				}

				if (data.type == 1) {
					var el = $('<div class="message"><span></span><p></p></div>')
					$("span", el).text(data.name)
					$("p", el).text(data.text)
					$('#messages').append(el)
				}

				if (data.type == 1 && data.from != user.id) {
					var da = new Object();
					da.type=5;
					da.subtype=1;
					da.status=1;
					da.mid=data.mid;
					da.cid=data.cid;
					da.status=1;
					da.uuid=data.uuid;
					da.from=data.to;
					da.to=data.from;
					console.log('send read ack: '+JSON.stringify(da));
					chatSocket.send(JSON.stringify(da))
				}
			}

			var handleReturnKey = function(e) {
				if(e.charCode == 13 || e.keyCode == 13) {
					e.preventDefault()
					sendMessage()
				}
			}

			$("#talk").keypress(handleReturnKey);
			$('#talk').bind('input propertychange', function() {
				var text = $("#talk").val();
				//console.log('text typing '+text);
				if (text.length == 1) {
					var rec = $("#recipients").val();
					var from = $("#from").val();
					var msg = JSON.stringify({"to":rec,"type":"9", "subtype":"0"});
					chatSocket.send(msg);
				}
			});
			

			chatSocket.onmessage = receiveEvent;
			chatSocket.onclose = onClose;
			chatSocket.onerror = onError;
			chatSocket.onopen = onOpen;
			
			$("#sendMsgBtn").click(function() {
				var message = $("#JsonMessage").val();
				console.log('chat message: '+message);
				chatSocket.send(message);
			})
}

$(function () {	
	attempts++;
	createWebSocket();
});