from pulsar import Function
from pulsar.schema import *
from collections import deque
import pandas as pd


def last_value(series):
    if series is None:
        print("Series is None ")
        return 0

    if series.empty:
        print("Series is empty ")
        return 0

    # Get the last value of the series
    last_val = series.iloc[-1]
    # Replace NaN with 0
    if pd.isna(last_val):
        print("Last value is NaN, returning 0")
        return 0
    else:
        return last_val


class Ticker(Record):
    sequence = Long()
    product_id = String()
    price = Float()
    latest_emwa = Float()
    latest_std = Float()
    latest_variance = Float()
    rolling_mean = Float()
    rolling_std = Float()
    rolling_variance = Float()
    time = String()
    millis = Long()


class Schema:
    schema = None

    def __init__(self, *args):
        self.schema = args[0]

    def __call__(self, f):
        def wrapped(*args):
            args = list(args)
            args[1] = self.schema.decode(args[1].encode())
            return self.schema.encode(f(*tuple(args))).decode("utf-8")

        return wrapped


class TickerStatsFunction(Function):
    def __init__(self):
        self.ticker_price_queues = {}

    def add_element(self, ticker_symbol, element):
        # If the deque for the stock symbol doesn't exist, create it
        if ticker_symbol not in self.ticker_price_queues:
            self.ticker_price_queues[ticker_symbol] = deque(maxlen=20)
        # Add element to the deque corresponding to the stock symbol
        self.ticker_price_queues[ticker_symbol].append(element)

    def get_prices_as_list(self, ticker_symbol):
        # If the deque for the stock symbol exists, convert it to a list
        if ticker_symbol in self.ticker_price_queues:
            l = list(self.ticker_price_queues[ticker_symbol])
            l.reverse()
            return l
        else:
            return None  # Return None if the deque doesn't exist for the given stock symbol

    def calculate_latest_metrics(self, span, ticker_symbol):
        # We need to reverse the list to get them in proper order, i.e. most recent price first
        data = self.get_prices_as_list(ticker_symbol)

        if data is None:
            print("Data is None, so return zeros")
            return 0, 0, 0

        series = pd.Series(data)

        # Calculate EWMA
        ewma = series.ewm(span=span, adjust=False).mean()

        # Calculate standard deviation
        std = series.ewm(span=span, adjust=False).std()

        # Calculate variance
        variance = std ** 2

        latest_ewma = last_value(ewma)
        latest_std = last_value(std)
        latest_variance = last_value(variance)
        return latest_ewma, latest_std, latest_variance

    def calculate_rolling_metrics(self, span, window, ticker_symbol):
        # We need to reverse the list to get them in proper order, i.e. most recent price first
        data = self.get_prices_as_list(ticker_symbol)

        if data is None:
            print("Rolling Data is None, so returning zeros")
            return 0, 0, 0

        series = pd.Series(data)

        rolling_mean = series.rolling(window=len(series)).mean()

        # Calculate rolling standard deviation with the specified window size
        rolling_std = series.rolling(window=len(series)).std()

        # Calculate rolling variance with the specified window size
        rolling_variance = rolling_std ** 2

        # Extract the latest value
        latest_mean = last_value(rolling_mean)
        latest_std = last_value(rolling_std)
        latest_variance = last_value(rolling_variance)

        return latest_mean, latest_std, latest_variance

    @Schema(JsonSchema(Ticker))
    def process(self, input, context):
        self.add_element(input.product_id, input.price)
        ewma, std, variance = self.calculate_latest_metrics(10, input.product_id)
        input.latest_emwa = ewma
        input.latest_std = std
        input.latest_variance = variance

        rolling_mean, rolling_std, rolling_variance = self.calculate_rolling_metrics(10, 5, input.product_id)
        input.rolling_mean = rolling_mean
        input.rolling_std = rolling_std
        input.rolling_variance = rolling_variance
        return input
