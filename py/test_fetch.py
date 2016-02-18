import requests
import json
import random
import uuid
import time

url = "http://modis.csail.mit.edu:10001/forecache/modis/fetch/"
current_milli_time = lambda: int(round(time.time() * 1000))
reset = lambda:  requests.get(url,params={"reset":True})
getts = lambda:  requests.get(url,params={"getts":True})
getallkeys = lambda: requests.get(url,params={"getallkeys":True})

reset()
#get tile structure
res = getts()
ts = json.loads(res.content)
tile_sizes = [1,5,10,20,50,100,150,200,225,250,275,300]

#update tile structure
def updateTileSize(tile_size):
  global ts
  ts["tileWidths"] = [tile_size,tile_size]
  res = requests.get(url,params={"setts":True,"ts":json.dumps(ts)})

#get the set of all tile keys to use for this test
updateTileSize(300)
res = getts()
print res.content
res = getallkeys()
keys = json.loads(res.content)
keymap = {} # use to find keys at a certain zoom level
for key in keys:
  if key["zoom"] not in keymap:
    keymap[key["zoom"]] = []
  keymap[key["zoom"]].append(key)

f = open('fetch_data.txt', 'w')
f.write( "\t".join(["tile_size","zoom","x","y","duration_ms","bytes","cached"]))
f.write("\n")
f.close()
for tile_size in tile_sizes:
  # make sure the previously cached tiles aren't being used
  reset()
  updateTileSize(tile_size)
  for zoom in keymap.keys():
    tpl = {} # track which tiles were requested
    for i in range(0,10):
      jmax = 20
      for j in range(0,jmax):
        tpos = random.randrange(0,len(keymap[zoom]))
        if tpos not in tpl:
          tpl[tpos] = True
          tilekey = keymap[zoom][tpos]
          tilename = str(tilekey["dimIndices"][0]) + "_" + str(tilekey["dimIndices"][1])
          s = current_milli_time()
          res = requests.get(url,params={"binary":True,"zoom":zoom,"tile_id":tilename,"requestid":uuid.uuid4()})
          e = current_milli_time()
          b = bytearray(res.content)
          if (len(b) >334) or (j == (jmax-1)):
            #not cached
            f = open('fetch_data.txt', 'a')
            f.write("\t".join([str(tile_size),str(zoom),str(tilekey["dimIndices"][0]),str(tilekey["dimIndices"][1]),str(e-s),str(len(b)),"n"]))
            f.write("\n")
            f.close()
            print "\t".join([str(tile_size),str(zoom),str(tilekey["dimIndices"][0]),str(tilekey["dimIndices"][1]),str(e-s),str(len(b)),"n"])

            #cached
            s = current_milli_time()
            res = requests.get(url,params={"binary":True,"zoom":zoom,"tile_id":tilename,"requestid":uuid.uuid4()})
            e = current_milli_time()
            b = bytearray(res.content)
            f = open('fetch_data.txt', 'a')
            f.write("\t".join([str(tile_size),str(zoom),str(tilekey["dimIndices"][0]),str(tilekey["dimIndices"][1]),str(e-s),str(len(b)),"y"]))
            f.write("\n")
            f.close()
            print "\t".join([str(tile_size),str(zoom),str(tilekey["dimIndices"][0]),str(tilekey["dimIndices"][1]),str(e-s),str(len(b)),"y"])


            break

#http://modis.csail.mit.edu:10001/forecache/modis/fetch/?reset=true
#http://modis.csail.mit.edu:10001/forecache/modis/fetch/?getts=true
#http://modis.csail.mit.edu:10001/forecache/modis/fetch/?setts=true&ts=%7B%22aggregationWindows%22%3A%5B%5B150%2C150%5D%2C%5B98%2C98%5D%2C%5B64%2C64%5D%2C%5B32%2C32%5D%2C%5B16%2C16%5D%2C%5B8%2C8%5D%2C%5B4%2C4%5D%2C%5B2%2C2%5D%2C%5B1%2C1%5D%5D%2C%22tileWidths%22%3A%5B300%2C300%5D%7D
#http://modis.csail.mit.edu:10001/forecache/modis/fetch/?binary=true&zoom=0&tile_id=0_0&requestid=544a9249-2e62-42a2-a5ea-28678900d5eb


#http://modis.csail.mit.edu:10001/forecache/modis/fetch/?binary=true&zoom=1&tile_id=0_0&requestid=64b61bdd-5235-41a4-90ac-e849c61b8ece
#http://modis.csail.mit.edu:10001/forecache/modis/fetch/?binary=true&zoom=1&tile_id=0_1&requestid=64b61bdd-5235-41a4-90ac-e849c61b8ece
#http://modis.csail.mit.edu:10001/forecache/modis/fetch/?binary=true&zoom=1&tile_id=1_0&requestid=64b61bdd-5235-41a4-90ac-e849c61b8ece
#http://modis.csail.mit.edu:10001/forecache/modis/fetch/?binary=true&zoom=1&tile_id=1_1&requestid=64b61bdd-5235-41a4-90ac-e849c61b8ece

#http://modis.csail.mit.edu:10001/forecache/modis/fetch/?binary=true&zoom=2&tile_id=1_1&requestid=34970e08-b523-4caa-8a9d-3e5ba551e540
#http://modis.csail.mit.edu:10001/forecache/modis/fetch/?binary=true&zoom=2&tile_id=1_0&requestid=34970e08-b523-4caa-8a9d-3e5ba551e540
#http://modis.csail.mit.edu:10001/forecache/modis/fetch/?binary=true&zoom=2&tile_id=0_1&requestid=34970e08-b523-4caa-8a9d-3e5ba551e540
#http://modis.csail.mit.edu:10001/forecache/modis/fetch/?binary=true&zoom=2&tile_id=0_0&requestid=34970e08-b523-4caa-8a9d-3e5ba551e540

#http://modis.csail.mit.edu:10001/forecache/modis/fetch/?binary=true&zoom=3&tile_id=2_2&requestid=a88d3d97-dde2-48ec-97cd-851bdcd4cc3f
#http://modis.csail.mit.edu:10001/forecache/modis/fetch/?binary=true&zoom=3&tile_id=2_1&requestid=a88d3d97-dde2-48ec-97cd-851bdcd4cc3f
#http://modis.csail.mit.edu:10001/forecache/modis/fetch/?binary=true&zoom=3&tile_id=1_2&requestid=a88d3d97-dde2-48ec-97cd-851bdcd4cc3f
#http://modis.csail.mit.edu:10001/forecache/modis/fetch/?binary=true&zoom=3&tile_id=1_1&requestid=a88d3d97-dde2-48ec-97cd-851bdcd4cc3f

#http://modis.csail.mit.edu:10001/forecache/modis/fetch/?binary=true&zoom=4&tile_id=5_5&requestid=4a764e89-d33e-4342-9a37-fdaa38cf9641
#http://modis.csail.mit.edu:10001/forecache/modis/fetch/?binary=true&zoom=4&tile_id=5_4&requestid=4a764e89-d33e-4342-9a37-fdaa38cf9641
#http://modis.csail.mit.edu:10001/forecache/modis/fetch/?binary=true&zoom=4&tile_id=4_5&requestid=4a764e89-d33e-4342-9a37-fdaa38cf9641
#http://modis.csail.mit.edu:10001/forecache/modis/fetch/?binary=true&zoom=4&tile_id=4_4&requestid=4a764e89-d33e-4342-9a37-fdaa38cf9641

#http://modis.csail.mit.edu:10001/forecache/modis/fetch/?getallkeys=true


