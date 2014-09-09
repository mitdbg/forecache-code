from app import app
#app.run(app.config['SERVER_HOST'],app.config['SERVER_PORT'],debug=app.config['DEBUG'])
app.run(debug=app.config['DEBUG'])
