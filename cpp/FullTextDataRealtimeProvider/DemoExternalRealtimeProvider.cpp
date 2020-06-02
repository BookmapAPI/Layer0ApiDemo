// Demo to use with FullTextDataRealtimeProvider that
// generates random data for instruments that you subscribe to.
// You can compile it with visual studio by launching
// "Developer Command Prompt" (e.g. for VS 2017 it's
// "Developer Command Prompt for VS 2017" and running "cl DemoExternalRealtimeProvider.cpp"
// in the folder with this cpp file.
// This code was tested under Windows with VS 2017, but you should be able to use similar
// (or exactly the same) approach with other platforms/compilers

#include <iostream>
#include <thread>
#include <chrono>
#include <map>
#include <mutex>
#include <string>
#include <cstdio>
#include <cstdlib>

using namespace std;

const int DEPTH_LEVELS_COUNT = 10;

// We don't really need to report time
// (bookmap will ignore it in realtimemode),
// but just for consistency
long long getTime() {
	long long ms = chrono::duration_cast<chrono::nanoseconds>(
		chrono::system_clock::now().time_since_epoch()).count();
	return ms;
}

const char * toString(bool b) {
	return b ? "true" : "false";
}

// Primitive way to generate JSON. Will break if strings contain some
// characters, like " or \n. Used for simplicity.
void onDepth(string alias, bool isBid, int price, int size) {
	printf("Depth {\"alias\":\"%s\",\"isBid\":%s,\"price\":%d,\"size\":%d,\"time\":%lld}\n",
		alias.c_str(), toString(isBid), price, size, getTime());
}

void onTrade(string alias, bool isBid, int price, int size) {
	printf("Trade {\"alias\":\"%s\",\"price\":%d,\"size\":%d,\"tradeInfo\":{\
\"isOtc\":false,\"isBidAggressor\":%s,\"isExecutionStart\":true,\"isExecutionEnd\":true}\
,\"time\":%lld}\n",
		alias.c_str(), price, size, toString(isBid), getTime());
}

void onInstrumentAdded(string alias, string symbol, string exchange, string type,
	double pips, double multiplier, string fullName, bool isFullDepth,
	double sizeMultiplier) {
	printf("InstrumentAdded {\"alias\":\"%s\",\"instrumentInfo\":\
{\"pips\":%lf,\"multiplier\":%lf,\"fullName\":\"%s\",\"isFullDepth\":%s,\
\"sizeMultiplier\":%lf,\"symbol\":\"%s\",\"exchange\":\"%s\",\"type\":\"%s\"},\
\"time\":%lld}\n",
		alias.c_str(), pips, multiplier, fullName.c_str(), toString(isFullDepth),
		sizeMultiplier, symbol.c_str(), exchange.c_str(), type.c_str(), getTime());
	// Flushing because otherwise user will have to wait while it's in buffer
	fflush(stdout);
}

void onInstrumentAlreadySubscribed(string symbol, string exchange, string type) {
	printf("InstrumentAlreadySubscribed {\"symbol\":\"%s\",\"exchange\":\"%s\",\
\"type\":\"%s\",\"time\":%lld}\n",
	symbol.c_str(), exchange.c_str(), type.c_str(), getTime());
	// Flushing because otherwise user will have to wait while it's in buffer
	fflush(stdout);
}

void onLoginSuccessful() {
	printf("LoginSuccessful {\"time\":%lld}\n", getTime());
	// Flushing because otherwise user will have to wait while it's in buffer
	fflush(stdout);
}

void onLoginFailed(string reason, string message) {
	printf("LoginFailed {\"reason\":\"%s\",\"message\":\"%s\",\"time\":%lld}\n",
		reason.c_str(), message.c_str(), getTime());
	// Flushing because otherwise user will have to wait while it's in buffer
	fflush(stdout);
}

struct Instrument {

	string alias;
	double pips;

	int basePrice;

	Instrument(string alias, double pips) {
		this->alias = alias;
		this->pips = pips;

		// Pick random price that will be used to generate the data
		// This is an integer representation of a price (before multiplying
		// by pips)
		this->basePrice = (int)(rand() % 10000 + 1000);
	}

	void generateData() {

		// Determining best bid/ask
		int bestBid = getBestBid();
		int bestAsk = getBestAsk();

		// Populating 10 levels to each side of best bid/best ask with
		// random data
		for (int i = 0; i < DEPTH_LEVELS_COUNT; ++i) {
			int levelsOffset = i;
			onDepth(alias, true, bestBid - levelsOffset, getRandomSize());
			onDepth(alias, false, bestAsk + levelsOffset, getRandomSize());
		}

		// Trade on best bid, ask agressor
		onTrade(alias, false, bestBid, 1);
		// Trade on best ask, bid agressor
		onTrade(alias, true, bestAsk, 1);

		// With 10% chance change BBO
		if (rand() % 100 < 10) {
			// 50% chance to move up, 50% to move down
			if (rand() % 100 > 50) {
				// Moving up - erasing best ask, erasing last reported bid
				// level (emulating exchange only reporting few levels)
				++basePrice;
				onDepth(alias, false, bestAsk, 0);
				onDepth(alias, true, bestBid - (DEPTH_LEVELS_COUNT - 1), 0);
				// Could also populate new best bid and add last best ask,
				// but this can be omitted - those will be populated during
				// next simulation step
			}
			else {
				// Moving down - erasing best bid, erasing last reported ask
				// level (emulating exchange only reporting few levels)
				--basePrice;
				onDepth(alias, true, bestBid, 0);
				onDepth(alias, false, bestAsk + (DEPTH_LEVELS_COUNT - 1), 0);
				// Could also populate new best ask and add last best bid,
				// but this can be omitted - those will be populated during
				// next simulation step
			}
		}
	}

	int getBestAsk() {
		return basePrice;
	}

	int getBestBid() {
		return getBestAsk() - 1;
	}

	int getRandomSize() {
		return (int)(1 + rand() % 10);
	}

};

bool closing = false;

mutex instrumentsMutex;
map<string, Instrument> instruments;

void login(string user, string password, bool demo) {
	// With real connection provider would attempt establishing connection here.
	bool isValid = "pass" == password && "user" == user && demo == true;

	if (isValid) {
		// Report succesful login
		onLoginSuccessful();
	}
	else {
		// Report failed login
		// Since we don't have proper json serializer in this example,
		// we have to escape newlines here
		onLoginFailed("WRONG_CREDENTIALS",
			"This provider only acepts following credentials:\\n\
username: user\\npassword: pass\\nis demo: checked");
	}
}

string createAlias(string symbol, string exchange, string type) {
	return symbol + "/" + exchange + "/" + type;
}

void subscribe(string symbol, string exchange, string type) {
	std::lock_guard<std::mutex> lock(instrumentsMutex);

	string alias = createAlias(symbol, exchange, type);
	if (instruments.find(alias) != instruments.end()) {
		onInstrumentAlreadySubscribed(symbol, exchange, type);
	}
	else {
		// We are performing subscription synchronously for simplicity,
		// but if subscription process takes long it's better to do it
		// asynchronously

		// Randomly determining pips. In reality it will be received
		// from external source
		double pips = rand() % 100 > 50 ? 0.5 : 0.25;

		instruments.emplace(alias, Instrument(alias, pips));

		onInstrumentAdded(alias, symbol, exchange, type, pips, 1, "Full name:" + alias, false, 1);
	}
}

void unsubscribe(string alias) {
	std::lock_guard<std::mutex> lock(instrumentsMutex);
	instruments.erase(alias);
}

void simulateStep() {
	std::lock_guard<std::mutex> lock(instrumentsMutex);
	for (auto entry : instruments) {
		entry.second.generateData();
	}
	// Instead of flushing every single update we flush at the end
	// This is a bit more performance-friendly approach
	// But this probably doesn't make a practical difference
	// as 200K+ events per second can be pushed both ways,
	// bottlenecking the actual processing.
	fflush(stdout);
}

void simulate() {
	while (!closing) {
		// Generate some data changes
		simulateStep();
		// Waiting a bit before generating more data.
		// Interruptable sleep would be better.
		// Downside of this approach is that bookmap will have to wait up to 1s to
		// shut down this thread.
		this_thread::sleep_for(chrono::milliseconds(1000));
	}
}

int main() {

	thread simulationThread(simulate);

	bool readSuccess;
	string command;
	do {
		readSuccess = !getline(cin, command).eof();
		if (!readSuccess) {
			// Do nothing. This is end of file.
			// (bookmap died without closing the module). 
			// If we ignore EOF subprocess might stay e.g.
			// if bookmap was killed via task manager.
		} else if (command == "login") {
			
			string user, password, demoString;
			getline(cin, user);
			getline(cin, password);
			getline(cin, demoString);
			bool demo = demoString == "true";

			login(user, password, demo);

		} else if (command == "subscribe") {

			string symbol, exchange, type;
			getline(cin, symbol);
			getline(cin, exchange);
			getline(cin, type);

			subscribe(symbol, exchange, type);

		} else if (command == "unsubscribe") {
			string alias;
			getline(cin, alias);
			unsubscribe(alias);
		}
	} while (command != "close" && readSuccess);

	closing = true;
	simulationThread.join();

	return 0;
}
