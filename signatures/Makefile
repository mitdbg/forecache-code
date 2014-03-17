CC := g++
SRCDIR := src
BUILDDIR := build
TARGETDIR := bin

SRCEXT := cpp
SOURCES := $(shell find $(SRCDIR) -type f -name *.$(SRCEXT))

OBJECTS := $(patsubst $(SRCDIR)/%,$(BUILDDIR)/%,$(SOURCES:.$(SRCEXT)=.o))
CFLAGS := -g # -Wall
LIB := -pthread -L lib -lpqxx -lboost_filesystem-mt -lboost_system-mt #-lboost_thread-mt 
INC := -Iinclude -I include/rapidjson -I/usr/include

all: analyzeTraces

analyzeTraces: $(BUILDDIR)/AnalyzeTraces.o $(BUILDDIR)/ComputeSignatures.o $(BUILDDIR)/NormalSignature.o
	$(CC) $^ -o $(TARGETDIR)/$@ $(LIB)

$(BUILDDIR)/%.o: $(SRCDIR)/%.$(SRCEXT)
	$(CC) $(CFLAGS) $(INC) -c -o $@ $<

clean:
	$(RM) $(BUILDDIR)/* $(TARGETDIR)/*

.PHONY: clean
