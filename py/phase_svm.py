from sklearn import svm
from sklearn import preprocessing
import csv

#hlens = [1,2,3,4,5,6,7,8,9,10]
hlens = [1,2]
users = ['146', '150', '123', '145', '140', '132', '141', '151', '144', '148', '121', '130', '124', '135', '134', '137', '139', '138']

for hlen in hlens:
  overall = 0.0
  total = 0.0
  for uid in users:
    X=[]
    y=[]
    testX=[]
    testy=[]
    with open('/Volumes/E/mit/vis/code/scalar-prefetch/code/harness/out_'+str(hlen)+'.tsv') as csvfile:
      reader = csv.DictReader(csvfile,delimiter='\t')
      for row in reader:
        #inp = [float(row['incount']),float(row['outcount']),float(row['pancount']),float(row['zoom'])]
        inp = [float(row['incount']),float(row['outcount']),float(row['pancount']),float(row['zoom']),float(row['x']),float(row['y'])]
        label = row['phase']
        if row['user'] == uid: # test data
          testX.append(inp)
          testy.append(label)
        else:
          X.append(inp)
          y.append(label)
    scaler = preprocessing.StandardScaler().fit(X)
    X_scaled = scaler.transform(X)
  
   #print len(X),",",len(y),X_scaled.shape

    clf = svm.SVC() # choose the SVM setup you want
    clf.fit(X_scaled, y)  # train the model
    accuracy = 0.0
  
    for i,row in enumerate(testX):
      row_scaled = scaler.transform(row)
      pred=clf.predict(row_scaled)[0]
      total +=1
      if pred == testy[i]:
        accuracy +=1
        overall +=1
    accuracy /= len(testy)
    print "accuracy for user '",uid,"':",accuracy
  print "overall accuracy for hlen="+str(hlen)+": "+str(overall/total)
