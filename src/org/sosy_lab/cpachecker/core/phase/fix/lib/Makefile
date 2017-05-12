CC=gcc
SLINK_FLAGS=-shared
LINK_OUT_FLAG=-o
CC_OUT_FLAG=-c
CC_EXTRA_FLAG=-Wall -fPIC
SO_EXT=.so
PREFIX=/usr

all: libTsmartIntFix$(SO_EXT)

libTsmartIntFix$(SO_EXT): intfix.o
	$(CC) $(SLINK_FLAGS) $(LINK_OUT_FLAG) $@ $^

intfix.o: intfix.c
	$(CC) $(CC_OUT_FLAG) $(CC_EXTRA_FLAG) $^

clean:
	@rm -f *.o *$(SO_EXT)

install:
	@mkdir -p $(PREFIX)/include
	@mkdir -p $(PREFIX)/lib
	@cp ./intfix.h $(PREFIX)/include/tsmartIntFix.h
	@cp ./libTsmartIntFix$(SO_EXT) $(PREFIX)/lib/libTsmartIntFix$(SO_EXT)
	@echo Tsmart Integer Fix library was successfully installed.

uninstall:
	@rm -f $(PREFIX)/include/tsmartIntFix.h
	@rm -f $(PREFIX)/lib/libTsmartIntFix$(SO_EXT)
	@echo Tsmart Integer Fix library was successfully uninstalled.