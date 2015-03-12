from flask import Flask, current_app, Blueprint,flash, session, request, render_template, g, redirect, send_file, url_for, send_from_directory
import json
import csv

mod = Blueprint('home',__name__)

@mod.route('/test',methods=["POST","GET"])
def get_page():
    return render_template('test.html')

@mod.route('/test/<user_id>/<task_id>',methods=["POST","GET"])
def visualize_trace(user_id,task_id):
    return json.dumps(filter(get_data(),user_id,task_id))

def filter(jsondata,uid,tid):
  newdata = []
  for r in jsondata:
    if r['userid'] == uid and r['taskname'] == tid:
      newdata.append(r)
  print "newdata"
  print newdata
  return newdata

def get_data():
  try:
    with open("user_traces.json",'r') as jsonf:
      return json.loads(jsonf.read())
  except:
    data = convert("user_traces.csv")
    return json.loads(data)

fieldnames=['userid','taskname','runid','stepid','move']
def convert(filename):
  print "Opening CSV file:",filename
  try:
    with open(filename, 'r') as f:
      csv_reader = csv.DictReader(f,fieldnames)
      json_filename = filename.split(".")[0]+".json"
      print "Saving JSON to file:",json_filename
      with open(json_filename,'w') as jsonf:
        data = json.dumps([r for r in csv_reader])
        jsonf.write(data)
        return data
  except:
    return None
