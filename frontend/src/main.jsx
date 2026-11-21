import React, { useState } from "react";
import { createRoot } from "react-dom/client";
import "./styles.css";

const attributes = [
  "addr_state",
  "application_type",
  "grade",
  "home_ownership",
  "loan_status",
  "purpose",
  "sub_grade",
  "term",
  "verification_status",
];

const apiUrl = import.meta.env.VITE_API_URL ?? "http://localhost:8080/api/loans";
const money = new Intl.NumberFormat("en-US", {
  style: "currency",
  currency: "USD",
  minimumFractionDigits: 2,
});
const compactMoney = new Intl.NumberFormat("en-US", {
  style: "currency",
  currency: "USD",
  maximumFractionDigits: 0,
  notation: "compact",
});

function LoanAmountChart({ results }) {
  const width = 1000;
  const height = 340;
  const left = 64;
  const right = 24;
  const top = 24;
  const bottom = 78;
  const plotWidth = width - left - right;
  const plotHeight = height - top - bottom;
  const amounts = results.map((row) => row.averageLoanAmount);
  const minAmount = Math.min(...amounts);
  const maxAmount = Math.max(...amounts);
  const binCount = Math.min(12, Math.max(1, Math.ceil(1 + Math.log2(results.length))));
  const range = maxAmount - minAmount;
  const binWidth = range === 0 ? 1 : range / binCount;
  const bins = Array.from({ length: range === 0 ? 1 : binCount }, (_, index) => ({
    lower: minAmount + index * binWidth,
    upper: range === 0 ? maxAmount : minAmount + (index + 1) * binWidth,
    count: 0,
  }));
  amounts.forEach((amount) => {
    const index = range === 0
      ? 0
      : Math.min(bins.length - 1, Math.floor((amount - minAmount) / binWidth));
    bins[index].count += 1;
  });
  const maxCount = Math.max(...bins.map((bin) => bin.count));
  const y = (count) => top + plotHeight - (count / maxCount) * plotHeight;
  const yTicks = Array.from({ length: Math.min(5, maxCount + 1) }, (_, index) => (
    Math.round((maxCount * index) / (Math.min(5, maxCount + 1) - 1))
  ));
  const gap = 3;
  const barWidth = plotWidth / bins.length;

  return (
    <div
      className="chart-scroll"
      role="img"
      aria-label={`Histogram of average loan amounts across ${results.length} attribute groups. Bars count groups within each loan amount range.`}
    >
      <svg
        className="loan-chart"
        viewBox={`0 0 ${width} ${height}`}
        width={width}
        height={height}
        aria-hidden="true"
      >
        {yTicks.map((tick) => (
          <g key={`y-${tick}`}>
            <line
              className="chart-grid"
              x1={left}
              x2={width - right}
              y1={y(tick)}
              y2={y(tick)}
            />
            <text className="chart-tick" x={left - 12} y={y(tick) + 4} textAnchor="end">
              {tick}
            </text>
          </g>
        ))}
        {bins.map((bin, index) => {
          const x = left + index * barWidth;
          const barHeight = (bin.count / maxCount) * plotHeight;
          const label = range === 0
            ? compactMoney.format(minAmount)
            : `${compactMoney.format(bin.lower)}–${compactMoney.format(bin.upper)}`;
          return (
            <g key={bin.lower}>
              <rect
                className="chart-bar"
                x={x + gap}
                y={y(bin.count)}
                width={Math.max(0, barWidth - gap * 2)}
                height={barHeight}
                rx="3"
              />
              <text
                className="chart-tick"
                transform={`translate(${x + barWidth / 2}, ${height - bottom + 16}) rotate(-35)`}
                textAnchor="end"
              >
                {label}
              </text>
              <title>
                {`${money.format(bin.lower)} to ${money.format(bin.upper)} average loan amount: ${bin.count} ${bin.count === 1 ? "group" : "groups"}`}
              </title>
            </g>
          );
        })}
        <text className="chart-axis-label" x={left + plotWidth / 2} y={height - 5} textAnchor="middle">
          Average loan amount range
        </text>
        <text
          className="chart-axis-label"
          transform={`translate(16, ${top + plotHeight / 2}) rotate(-90)`}
          textAnchor="middle"
        >
          Number of groups
        </text>
      </svg>
    </div>
  );
}

function App() {
  const [attribute, setAttribute] = useState("grade");
  const [results, setResults] = useState([]);
  const [searchedAttribute, setSearchedAttribute] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  async function search(event) {
    event.preventDefault();
    setLoading(true);
    setError("");
    setResults([]);
    setSearchedAttribute("");

    try {
      const response = await fetch(
        `${apiUrl}?attribute=${encodeURIComponent(attribute.trim())}`,
      );
      if (!response.ok) {
        const body = await response.json().catch(() => ({}));
        throw new Error(
          body.message || body.detail || body.error || "Unable to load loan statistics.",
        );
      }

      setResults(await response.json());
      setSearchedAttribute(attribute.trim());
    } catch (requestError) {
      setError(
        requestError.message === "Failed to fetch"
          ? "Unable to connect to the loan statistics service."
          : requestError.message,
      );
    } finally {
      setLoading(false);
    }
  }

  return (
    <main className="page">
      <header className="hero">
        <p className="eyebrow">Lending Club · 2017 Q4</p>
        <h1>Loan statistics</h1>
        <p className="intro">
          Explore how loan amounts vary across the available loan attributes.
        </p>
      </header>

      <section className="panel" aria-labelledby="search-title">
        <div className="section-heading">
          <div>
            <p className="eyebrow">Explore the data</p>
            <h2 id="search-title">Search by attribute</h2>
          </div>
        </div>
        <form className="search-form" onSubmit={search}>
          <label htmlFor="attribute">Attribute name</label>
          <div className="input-row">
            <input
              id="attribute"
              name="attribute"
              list="implemented-attributes"
              value={attribute}
              onChange={(event) => setAttribute(event.target.value)}
              placeholder="Enter an attribute, e.g. grade"
              required
            />
            <datalist id="implemented-attributes">
              {attributes.map((item) => (
                <option key={item} value={item} />
              ))}
            </datalist>
            <button type="submit" disabled={loading || !attribute.trim()}>
              {loading ? "Searching…" : "Search"}
            </button>
          </div>
        </form>

        <div className="attribute-list">
          <span className="list-label">Implemented attributes</span>
          <div className="chips">
            {attributes.map((item) => (
              <button
                className={`chip${attribute === item ? " selected" : ""}`}
                key={item}
                onClick={() => setAttribute(item)}
                type="button"
              >
                {item}
              </button>
            ))}
          </div>
        </div>
      </section>

      {error && (
        <div className="message error" role="alert">
          {error}
        </div>
      )}

      {searchedAttribute && !error && (
        <section className="panel results" aria-live="polite">
          <div className="section-heading results-heading">
            <div>
              <p className="eyebrow">Results for {searchedAttribute}</p>
              <h2>Loan amount by {searchedAttribute}</h2>
            </div>
            <span className="result-count">{results.length} groups</span>
          </div>
          {results.length === 0 ? (
            <p className="empty">No loan records were found for this attribute.</p>
          ) : (
            <>
              <div className="chart-section">
                <div className="chart-caption">
                  <strong>Histogram of group averages</strong>
                  <span>Each bar counts attribute groups whose average loan amount falls in that range.</span>
                </div>
                <LoanAmountChart results={results} />
              </div>
              <div className="table-scroll">
                <table>
                  <thead>
                    <tr>
                      <th scope="col">Attribute value</th>
                      <th scope="col">Min loan amount</th>
                      <th scope="col">Max loan amount</th>
                      <th scope="col">Average loan amount</th>
                      <th scope="col">Median loan amount</th>
                    </tr>
                  </thead>
                  <tbody>
                    {results.map((row) => (
                      <tr key={row.attributeName}>
                        <th scope="row">{row.attributeName}</th>
                        <td>{money.format(row.minLoanAmount)}</td>
                        <td>{money.format(row.maxLoanAmount)}</td>
                        <td>{money.format(row.averageLoanAmount)}</td>
                        <td>{money.format(row.medianLoanAmount)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </>
          )}
        </section>
      )}
    </main>
  );
}

createRoot(document.getElementById("root")).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>,
);
