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
setchunksize = lambda cs: requests.get(url,params={"setchunksize":True,"chunkSize":cs})

reset()
#get tile structure
res = getts()
ts = json.loads(res.content)
#tile size of 600 is prohibitively expensive
#tile_sizes = [100,200,300]
#chunk_sizes = [300,600,1200,2400]
tile_sizes=[300]
chunk_sizes = [300]

scidb_ver="14_12"

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

filename='cook_data_'+scidb_ver+'.txt'
f = open(filename, 'w')
f.write( "\t".join(["chunk_size","tile_size","zoom","x","y","duration_ms","bytes","cook_fetch_cached"]))
f.write("\n")
f.close()
for chunk_size in chunk_sizes:
  print "updating chunk size..."
  res = setchunksize(chunk_size)
  for tile_size in tile_sizes:
    # make sure the previously cooked tiles aren't being used
    reset()

    print "updating tile size..."
    updateTileSize(tile_size)
    #get the set of all tile keys to use for this test
    res = getts()
    print res.content
    print "getting all tile keys..."
    res = getallkeys()
    keys = json.loads(res.content)
    keymap = {} # use to find keys at a certain zoom level
    for key in keys:
      if key["zoom"] not in keymap:
        keymap[key["zoom"]] = []
      keymap[key["zoom"]].append(key)

    for zoom in [7]:
    #for zoom in reversed(keymap.keys()):
      print "evaluating chunk size: "+str(chunk_size)+", tile size: "+str(tile_size)+", zoom level: "+ str(zoom)
      tpl = {} # track which tiles were requested
      random.shuffle(keymap[zoom]) # shuffle the keys at this zoom level
      totalkeys = len(keymap[zoom])
      tpos = 0
      for i in range(0,3):
        jmax = 3
        for j in range(0,jmax):
          #tpos = random.randrange(0,len(keymap[zoom]))
          if (tpos not in tpl) and (tpos < totalkeys):
            tpl[tpos] = True
            tilekey = keymap[zoom][tpos]
            tilename = str(tilekey["dimIndices"][0]) + "_" + str(tilekey["dimIndices"][1])
            #remove before we start
            res = requests.get(url,params={"removetile":True,"zoom":zoom,"tile_id":tilename})

            # test cook time
            s = current_milli_time()
            res = requests.get(url,params={"binary":True,"zoom":zoom,"tile_id":tilename,"requestid":uuid.uuid4()})
            e = current_milli_time()
            b = bytearray(res.content)
            
            if (len(b) >334) or (j == (jmax-1)): # if this is valid
              f = open(filename, 'a')
              f.write("\t".join([str(chunk_size),str(tile_size),str(zoom),str(tilekey["dimIndices"][0]),str(tilekey["dimIndices"][1]),str(e-s),str(len(b)),"cook"]))
              f.write("\n")
              f.close()
              print "\t".join([str(chunk_size),str(tile_size),str(zoom),str(tilekey["dimIndices"][0]),str(tilekey["dimIndices"][1]),str(e-s),str(len(b)),"cook"])

              # do cached fetch
              s = current_milli_time()
              res = requests.get(url,params={"binary":True,"zoom":zoom,"tile_id":tilename,"requestid":uuid.uuid4()})
              e = current_milli_time()
              b = bytearray(res.content)
              f = open(filename, 'a')
              f.write("\t".join([str(chunk_size),str(tile_size),str(zoom),str(tilekey["dimIndices"][0]),str(tilekey["dimIndices"][1]),str(e-s),str(len(b)),"cached"]))
              f.write("\n")
              f.close()
              print "\t".join([str(chunk_size),str(tile_size),str(zoom),str(tilekey["dimIndices"][0]),str(tilekey["dimIndices"][1]),str(e-s),str(len(b)),"cached"])

              # do not-cached fetch
              reset() # reset will clear the cache
              s = current_milli_time()
              res = requests.get(url,params={"binary":True,"zoom":zoom,"tile_id":tilename,"requestid":uuid.uuid4()})
              e = current_milli_time()
              b = bytearray(res.content)
              f = open(filename, 'a')
              f.write("\t".join([str(chunk_size),str(tile_size),str(zoom),str(tilekey["dimIndices"][0]),str(tilekey["dimIndices"][1]),str(e-s),str(len(b)),"fetch"]))
              f.write("\n")
              f.close()
              print "\t".join([str(chunk_size),str(tile_size),str(zoom),str(tilekey["dimIndices"][0]),str(tilekey["dimIndices"][1]),str(e-s),str(len(b)),"fetch"])

              # now remove the tile we just cooked
              res = requests.get(url,params={"removetile":True,"zoom":zoom,"tile_id":tilename})
              #removed=res.content
              break
            # now remove the tile we just cooked
            res = requests.get(url,params={"removetile":True,"zoom":zoom,"tile_id":tilename})
            #removed=res.content
          tpos += 1

