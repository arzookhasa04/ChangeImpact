# ChangeImpact: System Limitations & Future Improvements

Never claim that test selection guarantees 100% defect detection. Every regression test selection strategy involves an inherent trade-off between execution latency and defect detection confidence.

This document honestly outlines the technical limitations of ChangeImpact and details the roadmap for future improvements.

---

## 1. Architectural & Algorithmic Limitations

### A. Static Dependency Analysis Blind Spots
Static dependency graphs capture compile-time and explicit architectural connections, but cannot fully resolve:
- **Java Reflection**: `Class.forName("...")` or dynamic method invocations.
- **Dependency Injection Frameworks**: Spring `@Autowired`, CDI, or Guice runtime bean bindings without explicit metadata annotations.
- **Dynamic Configuration & Feature Flags**: Logic paths enabled or disabled via database or runtime environment variables.
- **Polymorphic Dispatch**: Interfaces with dozens of implementations where static analysis cannot determine the precise runtime receiver without point-to-analysis.

### B. Coarse Component-Level Coverage Granularity
ChangeImpact operates primarily at the module/component level:
- If a test touches `PaymentGateway`, it is recorded as covering `PaymentGateway`.
- It does not distinguish whether the test exercised lines 1-10 or lines 200-250.
- As demonstrated in Scenario 2 of our evaluation, treating components as binary atomic units causes integration tests to be labeled redundant if all individual components are already touched by unit tests. This misses interface and serialization bugs that only occur during component interaction.

### C. Historical Failure Signal Constraints
Historical test data is valuable but imperfect:
- **Flaky Tests**: Tests that fail non-deterministically due to concurrency, timing, or network latency can artificially inflate the failure signal $F_2$, causing the engine to over-prioritize unstable tests at the expense of deterministic regression checks.
- **Cold-Start Bias**: Newly written tests have no execution history. While ChangeImpact mitigates this with an uncertainty adjustment ($F_4 = 1/\sqrt{N+1}$) and Laplace smoothing, true failure likelihood remains unknown until multiple builds have executed.

### D. Hard Budget Boundary Effects
Under hard execution budgets, tests are selected greedily by efficiency ($\text{MarginalGain} / \text{Cost}$).
- If an important integration test costs 450ms and the remaining budget is 400ms, the engine strictly rejects the test.
- The 0/1 knapsack restriction may leave "budget slack" (e.g., 50ms remaining that cannot accommodate any eligible test).

---

## 2. Engineering Roadmap & High-Value Priorities

To address these limitations, the following improvements represent the highest engineering priorities:

### Priority 1: Bytecode-Level Instrumentation (JaCoCo Adapter)
- Transition from coarse component-level mappings to basic-block and method-level coverage using JaCoCo execution data (`jacoco.exec`).
- Enables fine-grained change impact matching: if a method `calculateTax()` changes, only tests that actually execute that exact bytecode method are marked as candidates.

### Priority 2: Interaction & Contract Edge Modeling
- Introduce directed **interaction edges** between pairs of components (e.g., `PaymentGateway --[COMMUNICATES_WITH]--> FraudDetector`).
- Integration tests will be credited with covering the *interaction edge*, ensuring they are not discarded as redundant when individual components have already been exercised by unit tests.

### Priority 3: Flaky Test Classification Filter
- Incorporate a flakiness detection heuristic (e.g., consecutive pass-fail oscillations on identical Git commit hashes).
- Separate true regression failure signals from non-deterministic test noise.
