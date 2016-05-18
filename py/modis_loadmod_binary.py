import os
import sys
import time
import subprocess
import math
# Import smtplib for the actual sending function
import smtplib
# Import the email modules we'll need
from email.mime.text import MIMEText

'''
This script is specifically for loading data into SciDB, and onto modis and modis2 servers in the MIT DB group cluster.
This script was only tested for Ubuntu 12.04. Tthe Python script makes a lot of command line calls, and uses SciDB's
iquery command line interface.
These parameters are specific to the directory structures created on modis and modis2,
and definitely need to be modified if this script is to be used on different machines, with different file structures.
'''

if len(sys.argv) != 3:
	print "ERROR: incorrect number of inputs specified."
	print "usage: python modis_loadmod_binary.py <fileListFileName> <currinstance>"
	print "fileListFileName: name of file containing a list of MYD02 or MOD02 HDF filepaths, one HDF file per line"
	print "currinstance: the SciDB instance that will be in charge of the load"
	sys.exit(0)

#our time collection statistical format taken from man time but with command moved to the end
#timeFormat="%D\t%E\t%F\t%I\t%K\t%M\t%O\t%P\t%R\t%S\t%U\t%W\t%X\t%Z\t%c\t%e\t%k\t%p\t%r\t%s\t%t\t%w\t%x\t%C"
timeFormat="%e\t%M\t%P\t%x\t%C"
#%D Average size of the process's unshared data area, in Kbytes. 
#%E Elapsed real time (in [hours:]minutes:seconds). 
#%F Number of major page faults that occurred while the process was running. These are faults where the page has to be read in from disk.
#%I Number of file system inputs by the process. 
#%K Average total (data+stack+text) memory use of the process, in Kbytes. 
#%M Maximum resident set size of the process during its lifetime, in Kbytes.
#%O Number of file system outputs by the process.
#%P Percentage of the CPU that this job got, computed as (%U + %S) / %E.
#%R Number of minor, or recoverable, page faults. These are faults for pages that are not valid but which have not yet been claimed by other virtual pages. Thus the data in the page is still valid but the system tables must be updated. 
#%S Total number of CPU-seconds that the process spent in kernel mode. 
#%U Total number of CPU-seconds that the process spent in user mode. 
#%W Number of times the process was swapped out of main memory. 
#%X Average size of the process's shared text space, in Kbytes. 
#%Z (Not in tcsh.) System's page size, in bytes. This is a per-system constant, but varies between systems. 
#%c Number of times the process was context-switched involuntarily (because the time slice expired). 
#%e (Not in tcsh.) Elapsed real time (in seconds). 
#%k Number of signals delivered to the process. 
#%p (Not in tcsh.) Average size of the process's unshared stack space, in Kbytes. 
#%r Number of socket messages received by the process. 
#%s Number of socket messages sent by the process. 
#%t (Not in tcsh.) Average resident set size of the process, in Kbytes. 
#%w Number of waits: times that the program was context-switched voluntarily, for instance while waiting for an I/O operation to complete. 
#%x (Not in tcsh.) Exit status of the command. 
#%C (Not in tcsh.) Name and command-line arguments of the command being timed. 

getTimestamp=lambda: int(time.time())
envir=os.environ.copy()
currinstance=int(sys.argv[2])
script_sender="loadmod_py_"+ str(currinstance)
server="modis"
# Base folder for CSV output.
baseOutputFolder="/data/scidb/000/2/leilani/tmp"
if currinstance < 3: # stick with parameters for modis server
	script_sender =  script_sender + "@modis.csail.mit.edu"
else: # change parameters to match modis2 server
	server="modis2"
	script_sender = script_sender + "@modis2.csail.mit.edu"
	baseOutputFolder="/data/scidb/001/3/leilani/tmp"
script_recipient="leibatt@mit.edu"

# how many times can cleanup fail before we abort?
not_clean_threshold=2
scidb_heartbeat_threshold=3
failures_threshold=10

tsforlog=getTimestamp()
prefix="logs/" +server+ "_"+str(currinstance)+"_loadmod_binary"
timeLog=prefix+".log"
progressLog=prefix+".progress"



starttime=201400000000
endtime=201500000000
chunksize=100000 # this is for the load arrays
hostname="modis.csail.mit.edu"

# List of bands that should be imported.
bands=["1","2","3","4","5","6","7","8","9","10","11","12","13lo","13hi","14lo","14hi","15","16","17","18","19","20","21","22","23","24","25","26","27","28","29","30","31","32","33","34","35","36"]

#use this to get the max version for this array
def getMaxArrayVersion(arrayName):
	args=["/usr/bin/time","-f",timeFormat,"-a","-o",timeLog,"iquery","--host",hostname,"-o","tsv","-aq","project(filter(list('arrays',true),regex(name,'"+arrayName+"@.*')),name)"]
	result = executeProcessForResult(args,"could not get max version for array '"+arrayName+"'")
	if result is not None:
		maxver = -1
		lines = result.strip().split("\n")
		lines.pop(0) # remove the header
		for line in lines:
			if len(line) == 0:
				continue
			parts = line.split("@")
			#print "parts:",parts
			if len(parts) != 2:
				continue
			version = int(parts[1])
			if version > maxver:
				maxver = version
		return (maxver,len(lines))
	return (-1,0)

def removeVersions(arrayName):
	(versionNo,totalVersions) = getMaxArrayVersion(arrayName)
	if versionNo < 0:
		return False
	if totalVersions == 1: #no need to execute any more queries!
		return True
	args=["/usr/bin/time","-f",timeFormat, "-a","-o",timeLog,"iquery","--host",hostname,"-anq","remove_versions("+arrayName+","+str(versionNo)+")"]
	return executeProcess(args,"could not remove versions for array '"+arrayName+"'")

def loadArray(arrayName,loadSchema,fileName):
	# remove the first line of the file
	success = True
	args=["/usr/bin/time","-f",timeFormat,"-a","-o",timeLog,"sed","-i","1d",fileName]
	success &= executeProcess(args,"could not remove csv header for file '"+fileName+"'")
	if success:
		args=["/usr/bin/time","-f",timeFormat, "-a","-o",timeLog,"iquery","--host",hostname,"-anq","remove(load_"+str(currinstance)+"_"+arrayName+")"]
		#don't include this in success test, don't care if remove doesn't work
		executeProcess(args,"could not remove array 'load_"+str(currinstance)+"_"+arrayName+"'")
		args=["/usr/bin/time","-f",timeFormat,"-a","-o",timeLog,"iquery","--host",hostname,"-anq","create array load_"+str(currinstance)+"_"+arrayName+" "+loadSchema+""]
		#don't include this in success test, don't care if create array doesn't work
		executeProcess(args,"could not create array 'load_"+str(currinstance)+"_"+arrayName+"'")
		args=["/usr/bin/time","-f",timeFormat,"-a","-o",timeLog,"iquery","--host",hostname,"-anq","load(load_"+str(currinstance)+"_"+arrayName+",'"+fileName+"',"+str(currinstance)+",'csv:c')"]
		success &= executeProcess(args,"could not load data into array 'load_"+str(currinstance)+"_"+arrayName+"'")
	if success:
		args=["/usr/bin/time","-f",timeFormat,"-a","-o",timeLog,"iquery","--host",hostname,"-anq","insert(redimension(load_"+str(currinstance)+"_"+arrayName+","+arrayName+"),"+arrayName+")"]
		success &= executeProcess(args,"could not redimension array 'load_"+str(currinstance)+"_"+arrayName+"' and insert into array '"+arrayName+"'")
	#remove old versions of the array
	if success:
		removeVersions(arrayName) # be optimistic
		#success &= removeVersions(arrayName)
	return success

def loadArrayBinary(arrayName,loadSchema,binaryFormat,fileName):
	success = True
	args=["/usr/bin/time","-f",timeFormat,"-a","-o",timeLog,"iquery","--host",hostname,"-anq","remove(load_"+str(currinstance)+"_"+arrayName+")"]
	#don't include this in success test, don't care if remove doesn't work
	executeProcess(args,"could not remove array 'load_"+str(currinstance)+"_"+arrayName+"'")
	args=["/usr/bin/time","-f",timeFormat,"-a","-o",timeLog,"iquery","--host",hostname,"-anq","create array load_"+str(currinstance)+"_"+arrayName+" "+loadSchema+""]
	#don't include this in success test, don't care if create array doesn't work
	executeProcess(args,"could not create array 'load_"+str(currinstance)+"_"+arrayName+"'")
	args=["/usr/bin/time","-f",timeFormat,"-a","-o",timeLog,"iquery","--host",hostname,"-anq","load(load_"+str(currinstance)+"_"+arrayName+",'"+fileName+"',"+str(currinstance)+",'"+binaryFormat+"')"]
	success &= executeProcess(args,"could not load data into array 'load_"+str(currinstance)+"_"+arrayName+"'")
	if success:
		args=["/usr/bin/time","-f",timeFormat,"-a","-o",timeLog,"iquery","--host",hostname,"-anq","insert(redimension(load_"+str(currinstance)+"_"+arrayName+","+arrayName+"),"+arrayName+")"]
		success &= executeProcess(args,"could not redimension array 'load_"+str(currinstance)+"_"+arrayName+"' and insert into array '"+arrayName+"'")
	#remove old versions of the array
	if success:
		#success &= removeVersions(arrayName)
		removeVersions(arrayName) # be optimistic
	return success

def checkScidbHeartbeat(attempts_threshold):
	args=["iquery","--host",hostname,"-anq","list('instances')"]
	for i in range(0,attempts_threshold):
		if executeProcess(args,None):
			return True
		time.sleep(math.pow(3,i)) # exponential backoff
	return False

def checkDone(fileName):
	try:
		with open(progressLog,'r') as f:
			for line in f:
				fn=line.strip() # remove all whitespace and newline char
				if fileName == fn: # compare filenames
					return True
	except:
		pass
	return False

def writeProgressFile(baseFile):
	with open(progressLog,'a') as f:
		f.write(baseFile+"\n")

def executeProcessForResult(args,errorMessage):
	result = None
	try:
		#print "executing: "+subprocess.list2cmdline(args)
		o = subprocess.check_output(args,env=envir)
		result = o
	except subprocess.CalledProcessError as e:
		if errorMessage is not None:
			print errorMessage
		print e
		return result
	return result

def executeProcess(args,errorMessage):
	try:
		#print "executing: "+subprocess.list2cmdline(args)
		with open(os.devnull, "w") as devn: # pipe regular output to /dev/null
			p = subprocess.check_call(args,stdout=devn,env=envir)
	except subprocess.CalledProcessError as e:
		if errorMessage is not None:
			print errorMessage
		print e
		return False
	return True

# returns boolean representing whether the command was successful
def convertHdfToCsv(filePath):
	print "Converting HDF to CSV."
	#/usr/bin/time -f $timeFormat -a -o $timeLog /home/leilani/code/mod2csv/./mod2csv -b -q "$IN_FILE" -o "$baseOutputFolder"
	args=["/usr/bin/time","-f",""+timeFormat+"","-a","-o",timeLog,"/home/leilani/code/mod2csv/./mod2csv","-b","-q",filePath,"-o",baseOutputFolder]
	return executeProcess(args,"error occured converting file '"+filePath+"' to csv")

def loadGranuleMetadata(baseName):
	print "Loading granule metadata."
	loadSchema="<start_time:int64,platform_id:int64,resolution_id:int64,scans:uint8,track_measurements:uint16,scan_measurements:uint16,day_night_flag:string,file_id:string,geo_file_id:string>[i=0:*,"+str(chunksize)+",0]"
	return loadArray("granule_metadata",loadSchema,os.path.join(baseOutputFolder,baseName,"Granule_Metadata.csv"))

def loadBandMetadata(baseName):
	print "Loading band metadata."
	loadSchema="<start_time:int64,platform_id:int64,resolution_id:int64,band_id:int64,radiance_scale:double,radiance_offset:float,reflectance_scale:double,reflectance_offset:float,corrected_counts_scale:double,corrected_counts_offset:float,specified_uncertainty:float,uncertainty_scaling_factor:float>[i=0:*,"+str(chunksize)+",0]"
	binaryFormat="(int64, int32, int32, int32, float, float, float, float, float, float, float, float)"
	return loadArrayBinary("band_metadata",loadSchema,binaryFormat,os.path.join(baseOutputFolder,baseName,"Band_Metadata.bin"))

def loadGeodata(baseName):
	print "Loading geodata."
	loadSchema="<longitude_e4:int64,latitude_e4:int64,start_time:int64,platform_id:int64,resolution_id:int64,track_index:int16,scan_index:int16,height:int16,sensor_zenith:float,sensor_azimuth:float,range:uint32,solar_zenith:float,solar_azimuth:float,land_sea_mask:uint8>[i=0:*,"+str(chunksize)+",0]"
	binaryFormat="(int32, int32, int64, int32, int32, int32, int32, int32, float, float, int32, float, float, int16)"
	return loadArrayBinary("geodata",loadSchema,binaryFormat,os.path.join(baseOutputFolder,baseName,"Geodata.bin"))

def loadBandMeasurementData(baseName):
	loadSchema="<longitude_e4:int64,latitude_e4:int64,start_time:int64,platform_id:int64,resolution_id:int64,band_id:int64,si_value:uint16,radiance:double,reflectance:double,uncertainty_index:uint8,uncertainty_pct:float>[i=0:*,"+str(chunksize)+",0]"
	binaryFormat="(int32, int32, int64, int32, int32, int32, int32, float, float, int16, float)"
	success = True
	for band in bands:
		print "Loading band "+band+" measurements."
		success &= loadArrayBinary("band_"+band+"_measurements",loadSchema,binaryFormat,os.path.join(baseOutputFolder,baseName,"Band_"+band+"_Measurements.bin"))
		if not success:
			break
	return success

# cleans up binary/csv files created for specified granule
def cleanUp(baseName):
	print "Cleaning up."
	args=["/usr/bin/time","-f",timeFormat,"-a","-o",timeLog,"rm","-rf",os.path.join(baseOutputFolder,baseName)]
	return executeProcess(args,"error occured during cleanup for file '"+baseName+"'")


# returns size of raw data for specified granule
def getDirSizeForGranule(baseName):
	return getDirSize(os.path.join(baseOutputFolder,baseName))

# returns size of the specified directory in bytes
def getDirSize(dirpath):
	print "getting size of directory: "+dirpath
	args=["/usr/bin/time","-f",timeFormat,"-a","-o",timeLog,"du","-sb",dirpath]
	result = executeProcessForResult(args,"could not get size of directory '"+dirpath+"'")
	if (result is not None) and (len(result) > 0):
		tokens=result.split("\t")
		if len(tokens) == 2:
			return int(tokens[0])
	return -1

# used to alert me when things go wrong
def sendEmail(sender,recipient,subject,message):
	msg = MIMEText(message)
	msg['Subject'] = subject
	msg['From'] = sender
	s = smtplib.SMTP('localhost')
	s.sendmail(sender,[recipient],msg.as_string())
	s.quit()


# assumes there is a master file containing the list of files to process
def processFiles(fileListFileName):
	not_cleaned=0
	failed=0
	total = 0
	glens=[] # for capturing how long it takes to load each granule
	raw_data_sizes=[] # for capturing how big each granule is
	with open(fileListFileName) as fileList:
		for line in fileList:
			total += 1
			filePath = line.strip() # remove all whitespace and newline char
			# get base name from file path
			baseFile=os.path.basename(filePath)
			baseName=os.path.splitext(baseFile)[0]
			# has this base file been processed before?
			if not checkDone(baseName):
				# is this a real file?
				if not os.path.isfile(filePath):
					print "ERROR: File "+filePath+" does not exist."
					failed+=1
					continue
				# is SciDB running?
				if not checkScidbHeartbeat(scidb_heartbeat_threshold):
					print "ERROR: could not reach SciDB."
					sendEmail(script_sender,script_recipient,"SciDB unreachable ","ERROR: could not reach SciDB. aborting load for SciDB instance '"+str(currinstance)+"', on server '"+server+"'.")
					sys.exit(1)
				print "Processing file: "+filePath
				gstart=getTimestamp()
				success = True
				# Convert HDF to CSV.
				converted = convertHdfToCsv(filePath)
				success &= converted
				if success:
					# Load Granule Metadata.
					success &= loadGranuleMetadata(baseName)
				if success:
					# Load Band Metadata.
					success &= loadBandMetadata(baseName)
				if success:
					# Load Geodata.
					success &= loadGeodata(baseName)
				if success:
					# Load Measurement Data.
					success &= loadBandMeasurementData(baseName)
				# compute raw granule size before cleanup
				rawsize=getDirSizeForGranule(baseName)
				# Clean up
				cleaned = cleanUp(baseName)
				success &= cleaned
				if not cleaned:
					not_cleaned += 1
					if not_cleaned > not_clean_threshold:
						print "ERROR: too many cleanup errors. aborting load."
						sendEmail(script_sender,script_recipient,"clean up failed on "+server,"ERROR: too many cleanup errors. aborting load for SciDB instance '"+str(currinstance)+"', on server '"+server+"'.")
						sys.exit(1)
				if success:
					# record this base name in the progress log
					writeProgressFile(baseName)
				        gend=getTimestamp()
					glens.append(gend-gstart)
					print "individual load completed in "+str(gend-gstart)+" seconds."
					# track granule sizes
					raw_data_sizes.append(rawsize)
					print "loaded "+str(rawsize)+" bytes of raw data for this granule."
				else:
					failed += 1
					if failed > failures_threshold:
						print "ERROR: too many failed files. aborting load."
						sendEmail(script_sender,script_recipient,"too many failures on "+server,"ERROR: too many failures ("+str(failed)+" total failures). aborting load for SciDB instance '"+str(currinstance)+"', on server '"+server+"'.")
						sys.exit(1)
	return [failed,total,glens,raw_data_sizes]
# timestamp in seconds
start=getTimestamp()
final_stats=processFiles(sys.argv[1])
#timestamp in seconds
end=getTimestamp()
#duration in seconds
elapsed=end - start
print "executed load in "+str(elapsed)+" seconds."
print "loaded granules in "+str(sum(final_stats[2]) / float(len(final_stats[2])))+" seconds on average."
print "loaded "+str(sum(final_stats[3]))+" total bytes of raw data."
sendEmail(script_sender,script_recipient,"data load finished","Finished loading MODIS data specified in file '"+sys.argv[1]+"', using SciDB instance '"+str(currinstance)+"', on server '"+server+"'. "+str(final_stats[0])+" files failed to load out of "+str(final_stats[1])+". Executed load in "+str(elapsed)+" seconds. Loaded granules in "+str(sum(final_stats[2]) / float(len(final_stats[2])))+" seconds on average. loaded "+str(sum(final_stats[3]))+" total bytes of raw data.")

