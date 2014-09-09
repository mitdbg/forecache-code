from flask import Flask

app = Flask(__name__,static_folder='static')

#add app configurations
app.config.from_object('config')

#must happen last
from views import mod as baseModule
app.register_blueprint(baseModule)

