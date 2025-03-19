let indexJs;

$(function() {

    indexJs = {
        v: {
            socket : {},
            sessionId : new Date().getTime(),
            currentAiMessage : null,
        },

        c: {
            connectWebSocket : function() {
                console.log(indexJs.v.sessionId)
                indexJs.v.socket = new WebSocket('ws://localhost:8088/chat');

                indexJs.v.socket.onmessage = function(event) {
                    if (event.data === "[END]") {
                        indexJs.v.currentAiMessage = null;
                        indexJs.v.socket.close();
                        return;
                    }
                    indexJs.f.updateAiMessage(event.data);
                };

                indexJs.v.socket.onerror = function(error) {
                    console.error(error);
                };
            }
        },

        f: {
            send : function() {
                const message = $('input#userInput').val();

                if (message == '') {
                    return false;
                }

                $('input#userInput').val('');
                indexJs.f.addMessage(message);

                indexJs.c.connectWebSocket();
                indexJs.v.socket.onopen = function() {
                    indexJs.v.socket.send(JSON.stringify({sessionId: indexJs.v.sessionId, message: message}));
                };
            },

            addMessage : function(text) {
                const chatBox = $('div#chatBox');
                const messageDiv = $("<div>").addClass("message").addClass('user');
                const bubble = $("<div>").addClass("bubble").text(text);

                messageDiv.append(bubble);
                chatBox.append(messageDiv);
                chatBox.scrollTop(chatBox[0].scrollHeight);
            },

            updateAiMessage : function(text) {
                if (!indexJs.v.currentAiMessage) {
                    const chatBox = $("div#chatBox");
                    indexJs.v.currentAiMessage = $("<div>").addClass("message ai");
                    const bubble = $("<div>").addClass("bubble").text(text);
                    indexJs.v.currentAiMessage.append(bubble);
                    chatBox.append(indexJs.v.currentAiMessage);
                } else {
                    indexJs.v.currentAiMessage.find(".bubble").append(text);
                }
                $("div#chatBox").scrollTop($("div#chatBox")[0].scrollHeight);
            },
        },

        event: function() {
            $('button[name=send]').on('click', function(e) {
                indexJs.f.send();
            });

            $("input").on('keyup',function(e){
                if (window.event.keyCode == 13) {
                    indexJs.f.send();
                }
            });
        },

        init: function() {
            indexJs.event();
        }
    }

    indexJs.init();

});
