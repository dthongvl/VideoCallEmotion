var app = require('express')();
var server = require('http').Server(app);
var io = require('socket.io')(server);

server.listen(3000);

var clients = {};

app.get('/', function (req, res) {
  res.sendFile(__dirname + '/index.html');
});

io.on('connection', function (socket) {
  console.log(socket.id);
  socket.on('register', function (data) {
    console.log(data);
    clients[data] = socket;
    socket.emit('register', true);
  });
  socket.on('message', function (data) {
  	console.log(data);
	for (var clientName in clients) {
	  	if (clientName === data['to']) {
	  		clients[clientName].emit('message', data);
	  	}
	}
  });
});