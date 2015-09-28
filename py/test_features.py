from sklearn import svm
from sklearn import preprocessing
import csv

#hlens = [1,2,3,4,5,6,7,8,9,10]
#hlens = [1,2]
hlens = [1]
users = ['146', '150', '123', '145', '140', '132', '141', '151', '144', '148', '121', '130', '124', '135', '134', '137', '139', '138']
tasks=['task1','task2','task3']
feature_labels=["incount","outcount","pancount","zoom","x","y"]

for toRemove in range(0,6):
  a = 0
  t=0
  for task in tasks:
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
            #inp = [float(row['incount']),float(row['outcount']),float(row['pancount']),float(row['zoom']),float(row['x']),float(row['y'])]
            #del inp[toRemove]
            inp=[float(row[feature_labels[toRemove]])]
            label = row['phase']
            if row['taskname'] != task:
              continue
            if row['user'] == uid: # test data
              testX.append(inp)
              testy.append(label)
            else:
              X.append(inp)
              y.append(label)
        scaler = preprocessing.StandardScaler().fit(X)
        #print scaler
        X_scaled = scaler.transform(X)
    
       #print len(X),",",len(y),X_scaled.shape
  
        #clf = svm.SVC(gamma=0.07,C=1,verbose=True) # choose the SVM setup you want
        clf = svm.SVC(gamma=0.07,C=1,verbose=False) # choose the SVM setup you want
        clf.fit(X_scaled, y)  # train the model
        #print clf
        #print clf.get_params()
        accuracy = 0.0
    
        for i,row in enumerate(testX):
          row_scaled = scaler.transform(row)
          pred=clf.predict(row_scaled)[0]
          total +=1
          t+=1
          if pred == testy[i]:
            accuracy +=1
            overall +=1
            a+=1
        accuracy /= len(testy)
        #print "accuracy for user '",uid,"' and task '"+task+"':",accuracy
      #print "overall accuracy for task='"+task+"',hlen="+str(hlen)+": "+str(overall/total)
  #print "final accuracy:"+str(1.0*a/t)+" when removing feature '"+feature_labels[toRemove]+"'"
  print "final accuracy:"+str(1.0*a/t)+" for feature '"+feature_labels[toRemove]+"'"



a=0
t=0
print "without XY"
for task in tasks:
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
          inp = [float(row['incount']),float(row['outcount']),float(row['pancount']),float(row['zoom'])]
          label = row['phase']
          if row['taskname'] != task:
            continue
          if row['user'] == uid: # test data
            testX.append(inp)
            testy.append(label)
          else:
            X.append(inp)
            y.append(label)
      scaler = preprocessing.StandardScaler().fit(X)
      #print scaler
      X_scaled = scaler.transform(X)
    
     #print len(X),",",len(y),X_scaled.shape
  
      #clf = svm.SVC(gamma=0.07,C=1,verbose=True) # choose the SVM setup you want
      clf = svm.SVC(gamma=0.07,C=1,verbose=False) # choose the SVM setup you want
      clf.fit(X_scaled, y)  # train the model
      #print clf
      #print clf.get_params()
      accuracy = 0.0
    
      for i,row in enumerate(testX):
        row_scaled = scaler.transform(row)
        pred=clf.predict(row_scaled)[0]
        total +=1
        t+=1
        if pred == testy[i]:
          accuracy +=1
          overall +=1
          a+=1
      accuracy /= len(testy)
print "final accuracy:"+str(1.0*a/t)


print "without position"
a = 0
t=0
for task in tasks:
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
          inp = [float(row['incount']),float(row['outcount']),float(row['pancount'])]
          label = row['phase']
          if row['taskname'] != task:
            continue
          if row['user'] == uid: # test data
            testX.append(inp)
            testy.append(label)
          else:
            X.append(inp)
            y.append(label)
      scaler = preprocessing.StandardScaler().fit(X)
      #print scaler
      X_scaled = scaler.transform(X)
    
     #print len(X),",",len(y),X_scaled.shape
  
      #clf = svm.SVC(gamma=0.07,C=1,verbose=True) # choose the SVM setup you want
      clf = svm.SVC(gamma=0.07,C=1,verbose=False) # choose the SVM setup you want
      clf.fit(X_scaled, y)  # train the model
      #print clf
      #print clf.get_params()
      accuracy = 0.0
    
      for i,row in enumerate(testX):
        row_scaled = scaler.transform(row)
        pred=clf.predict(row_scaled)[0]
        total +=1
        t+=1
        if pred == testy[i]:
          accuracy +=1
          overall +=1
          a+=1
      accuracy /= len(testy)
print "final accuracy:"+str(1.0*a/t)

a=0
t=0
print "without zoom"
for task in tasks:
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
          inp = [float(row['incount']),float(row['outcount']),float(row['pancount']),float(row['x']),float(row['y'])]
          label = row['phase']
          if row['taskname'] != task:
            continue
          if row['user'] == uid: # test data
            testX.append(inp)
            testy.append(label)
          else:
            X.append(inp)
            y.append(label)
      scaler = preprocessing.StandardScaler().fit(X)
      #print scaler
      X_scaled = scaler.transform(X)
    
     #print len(X),",",len(y),X_scaled.shape
  
      #clf = svm.SVC(gamma=0.07,C=1,verbose=True) # choose the SVM setup you want
      clf = svm.SVC(gamma=0.07,C=1,verbose=False) # choose the SVM setup you want
      clf.fit(X_scaled, y)  # train the model
      #print clf
      #print clf.get_params()
      accuracy = 0.0
    
      for i,row in enumerate(testX):
        row_scaled = scaler.transform(row)
        pred=clf.predict(row_scaled)[0]
        total +=1
        t+=1
        if pred == testy[i]:
          accuracy +=1
          overall +=1
          a+=1
      accuracy /= len(testy)
print "final accuracy:"+str(1.0*a/t)

