# Asset Management

← [Wiki Home](HOME.md)

---

## Overview

The Asset module tracks your investment portfolio — stocks, ETFs, mutual funds, bonds, cryptocurrencies, commodities, real estate investments, vehicles, jewellery, and collectibles. Live market prices are fetched automatically for assets with a ticker symbol.

---

## Asset Types

| Type        | Examples                            |
| ----------- | ----------------------------------- |
| Stock       | AAPL, MSFT, GOOGL                   |
| ETF         | SPY, QQQ, VTI                       |
| Mutual Fund | VFIAX, FXAIX                        |
| Bond        | US Treasury bonds, corporate bonds  |
| Crypto      | BTC, ETH                            |
| Commodity   | Gold, Silver, Oil                   |
| Real Estate | REITs, rental property shares       |
| Vehicle     | Cars, motorcycles, boats            |
| Jewelry     | Watches, rings, gemstones           |
| Collectible | Art, antiques, coins, trading cards |
| Other       | Anything else                       |

---

## Asset Condition (Physical Assets)

For physical assets (vehicles, jewellery, collectibles), you can record the item’s condition:

| Condition | Description                    |
| --------- | ------------------------------ |
| New       | Brand new, unused              |
| Excellent | Near-perfect, light use        |
| Good      | Normal wear                    |
| Fair      | Visible wear, fully functional |
| Poor      | Significant wear or damage     |

---

## Key Fields

| Field          | Notes                                          |
| -------------- | ---------------------------------------------- |
| Name           | A label for this asset                         |
| Symbol         | Ticker (e.g., AAPL) for market-tracked assets  |
| Type           | Choose from the asset types above              |
| Quantity       | Number of units or shares                      |
| Purchase Price | Cost per unit when you bought it               |
| Current Price  | Updated automatically from live market data    |
| Currency       | The currency the asset is denominated in       |
| Purchase Date  | Used to calculate gain/loss and holding period |
| Notes          | Optional free-text notes                       |

---

## Live Price Updates

For assets with a ticker symbol, Open-Finance fetches current market prices daily. You can also trigger an immediate price refresh from the asset detail page using the **Refresh Price** button.

See [Market Data](market-data.md) for details on supported symbols and price sources.

---

## Portfolio Metrics

The dashboard provides several portfolio views:

- **Asset Allocation** — breakdown by asset type (pie chart) with total portfolio value and unrealised gain/loss per asset.
- **Portfolio Performance** — time-weighted and money-weighted returns over a selected period.

---

## Related Pages

- [Market Data](market-data.md)
- [Real Estate](real-estate.md)
- [Dashboard](dashboard.md)
