import json

with open ('states2.json','w') as output:
  with open ('states.json','r') as dfile:
    data = json.load(dfile)
    for state in data.keys():
      coords = data[state]["Coordinates"] # list of lat/lng pairs
      for pair in coords:
        lat = float(pair["lat"])*10000.0
        lng = float(pair["lng"])*10000.0 # to match modis data
        lat2 = lat + 900000
        lng2 = lng + 1800000 # shifted so min value = 0
        pair["newlat"]=lat
        pair["newlng"]=lng
        pair["newlat2"]=lat2
        pair["newlng2"]=lng2
        #output.write(",".join([str(lat),str(lng),"\""+state+"\""])+"\n")
    output.write(json.dumps(data))
