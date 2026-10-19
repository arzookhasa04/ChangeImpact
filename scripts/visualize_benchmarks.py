#!/usr/bin/env python3
"""
ChangeImpact Benchmark Visualizer
Reads actual benchmark CSV results and generates publication-grade evaluation charts.
"""

import argparse
import os
import sys
import pandas as pd
import matplotlib.pyplot as plt
import matplotlib.ticker as ticker

# Style configuration
plt.style.use('seaborn-v0_8-whitegrid' if 'seaborn-v0_8-whitegrid' in plt.style.available else 'default')
plt.rcParams['font.sans-serif'] = 'Arial'
plt.rcParams['axes.edgecolor'] = '#cccccc'
plt.rcParams['axes.linewidth'] = 0.8
plt.rcParams['grid.color'] = '#e0e0e0'
plt.rcParams['grid.linestyle'] = '--'

STRATEGY_COLORS = {
    'RISK_AWARE_BUDGETED': '#1f77b4',  # Deep Blue
    'GREEDY_COST_ONLY': '#ff7f0e',      # Orange
    'GREEDY_FAILURE_ONLY': '#2ca02c',   # Green
    'DEPENDENCY_BASED': '#9467bd',      # Purple
    'FULL_SUITE': '#d62728'             # Crimson
}

STRATEGY_LABELS = {
    'RISK_AWARE_BUDGETED': 'Risk-Aware Budgeted (Proposed)',
    'GREEDY_COST_ONLY': 'Greedy Cost-Only Baseline',
    'GREEDY_FAILURE_ONLY': 'Greedy Failure-Only Baseline',
    'DEPENDENCY_BASED': 'Dependency-Based (Unbudgeted)',
    'FULL_SUITE': 'Full Suite Baseline'
}

def plot_budget_vs_fault_detection(df, output_dir):
    """Plot Injected Fault Detection Rate (%) vs Execution Budget (ms)."""
    plt.figure(figsize=(9, 5.5), dpi=300)
    
    # Filter budgeted strategies
    budgeted = df[df['strategy_name'].isin(['RISK_AWARE_BUDGETED', 'GREEDY_COST_ONLY', 'GREEDY_FAILURE_ONLY'])].copy()
    grouped = budgeted.groupby(['strategy_name', 'budget_ms'])['fault_detection_rate_pct'].mean().reset_index()

    for strat in ['RISK_AWARE_BUDGETED', 'GREEDY_COST_ONLY', 'GREEDY_FAILURE_ONLY']:
        subset = grouped[grouped['strategy_name'] == strat].sort_values('budget_ms')
        if not subset.empty:
            plt.plot(subset['budget_ms'], subset['fault_detection_rate_pct'],
                     marker='o', linewidth=2.4, markersize=8,
                     label=STRATEGY_LABELS.get(strat, strat),
                     color=STRATEGY_COLORS.get(strat, '#333333'))

    # Plot Full Suite and Dependency horizontal references
    full_mean = df[df['strategy_name'] == 'FULL_SUITE']['fault_detection_rate_pct'].mean()
    dep_mean = df[df['strategy_name'] == 'DEPENDENCY_BASED']['fault_detection_rate_pct'].mean()
    
    plt.axhline(full_mean, color=STRATEGY_COLORS['FULL_SUITE'], linestyle=':', linewidth=1.8,
                label=f'Full Suite Mean ({full_mean:.1f}%)')
    plt.axhline(dep_mean, color=STRATEGY_COLORS['DEPENDENCY_BASED'], linestyle='--', linewidth=1.8,
                label=f'Dependency-Based Mean ({dep_mean:.1f}%)')

    plt.title('Fault Detection Rate vs. Time Budget Constraint', fontsize=14, fontweight='bold', pad=12)
    plt.xlabel('Execution Time Budget (ms)', fontsize=11, labelpad=8)
    plt.ylabel('Injected Fault Detection Rate (%)', fontsize=11, labelpad=8)
    plt.ylim(0, 105)
    plt.legend(frameon=True, facecolor='white', framealpha=0.9, fontsize=10, loc='lower right')
    plt.tight_layout()

    out_file = os.path.join(output_dir, 'budget_vs_fault_detection.png')
    plt.savefig(out_file)
    plt.close()
    print(f"  [+] Chart generated: {out_file}")

def plot_efficiency_frontier(df, output_dir):
    """Plot Efficiency Frontier: Runtime Reduction (%) vs Fault Detection Rate (%)."""
    plt.figure(figsize=(9, 5.5), dpi=300)

    summary = df.groupby('strategy_name').agg({
        'time_reduction_pct': 'mean',
        'fault_detection_rate_pct': 'mean'
    }).reset_index()

    for _, row in summary.iterrows():
        strat = row['strategy_name']
        plt.scatter(row['time_reduction_pct'], row['fault_detection_rate_pct'],
                    s=220, color=STRATEGY_COLORS.get(strat, '#333333'),
                    edgecolor='black', linewidth=1.2, zorder=5,
                    label=STRATEGY_LABELS.get(strat, strat))
        
        offset_y = 3 if strat != 'GREEDY_COST_ONLY' else -5
        plt.annotate(f"{row['fault_detection_rate_pct']:.1f}% detect / {row['time_reduction_pct']:.1f}% saved",
                     (row['time_reduction_pct'], row['fault_detection_rate_pct'] + offset_y),
                     fontsize=9, ha='center', weight='semibold')

    plt.title('Trade-Off Frontier: Runtime Reduction vs. Defect Detection', fontsize=14, fontweight='bold', pad=12)
    plt.xlabel('Execution Time Reduction Relative to Full Suite (%)', fontsize=11, labelpad=8)
    plt.ylabel('Fault Detection Rate (%)', fontsize=11, labelpad=8)
    plt.xlim(-5, 100)
    plt.ylim(0, 110)
    plt.legend(frameon=True, facecolor='white', framealpha=0.9, fontsize=9.5, loc='lower left')
    plt.tight_layout()

    out_file = os.path.join(output_dir, 'strategy_efficiency_frontier.png')
    plt.savefig(out_file)
    plt.close()
    print(f"  [+] Chart generated: {out_file}")

def plot_affected_coverage(df, output_dir):
    """Plot Affected-Component Coverage (%) across strategies and budgets."""
    plt.figure(figsize=(9, 5.5), dpi=300)

    budgeted = df[df['strategy_name'].isin(['RISK_AWARE_BUDGETED', 'GREEDY_COST_ONLY', 'GREEDY_FAILURE_ONLY'])].copy()
    grouped = budgeted.groupby(['strategy_name', 'budget_ms'])['affected_coverage_pct'].mean().reset_index()

    for strat in ['RISK_AWARE_BUDGETED', 'GREEDY_COST_ONLY', 'GREEDY_FAILURE_ONLY']:
        subset = grouped[grouped['strategy_name'] == strat].sort_values('budget_ms')
        if not subset.empty:
            plt.plot(subset['budget_ms'], subset['affected_coverage_pct'],
                     marker='s', linewidth=2.2, markersize=7,
                     label=STRATEGY_LABELS.get(strat, strat),
                     color=STRATEGY_COLORS.get(strat, '#333333'))

    plt.title('Affected Component Coverage under Time Budgets', fontsize=14, fontweight='bold', pad=12)
    plt.xlabel('Execution Time Budget (ms)', fontsize=11, labelpad=8)
    plt.ylabel('Affected Components Coverage (%)', fontsize=11, labelpad=8)
    plt.ylim(0, 105)
    plt.legend(frameon=True, facecolor='white', framealpha=0.9, fontsize=10, loc='lower right')
    plt.tight_layout()

    out_file = os.path.join(output_dir, 'affected_component_coverage.png')
    plt.savefig(out_file)
    plt.close()
    print(f"  [+] Chart generated: {out_file}")

def plot_selection_ratios(df, output_dir):
    """Plot Test Selection Ratio (%) across scenarios."""
    plt.figure(figsize=(10, 5.5), dpi=300)

    scenarios = df['scenario_id'].unique()
    strategies = ['RISK_AWARE_BUDGETED', 'GREEDY_COST_ONLY', 'DEPENDENCY_BASED']
    
    # Average across budgets for budgeted strategies
    pivot_data = {}
    for strat in strategies:
        sub = df[df['strategy_name'] == strat]
        means = sub.groupby('scenario_id')['selection_ratio'].mean() * 100
        pivot_data[STRATEGY_LABELS.get(strat, strat)] = [means.get(scn, 0) for scn in scenarios]

    plot_df = pd.DataFrame(pivot_data, index=scenarios)
    ax = plot_df.plot(kind='bar', figsize=(10, 5.5), width=0.75,
                      color=[STRATEGY_COLORS[s] for s in strategies])

    plt.title('Test Suite Selection Ratio by Scenario', fontsize=14, fontweight='bold', pad=12)
    plt.xlabel('Scenario', fontsize=11, labelpad=8)
    plt.ylabel('Test Selection Ratio (%)', fontsize=11, labelpad=8)
    plt.ylim(0, 100)
    plt.xticks(rotation=15, ha='right')
    plt.legend(frameon=True, facecolor='white', framealpha=0.9, fontsize=9.5)
    plt.tight_layout()

    out_file = os.path.join(output_dir, 'selection_ratio_comparison.png')
    plt.savefig(out_file)
    plt.close()
    print(f"  [+] Chart generated: {out_file}")

def main():
    parser = argparse.ArgumentParser(description="Generate charts from ChangeImpact benchmark CSV.")
    parser.add_argument("--csv", required=True, help="Path to benchmark_results.csv")
    parser.add_argument("--output-dir", default="benchmarks/results/charts", help="Output directory for PNG charts")
    args = parser.parse_args()

    if not os.path.exists(args.csv):
        print(f"Error: CSV file not found at {args.csv}")
        sys.exit(1)

    os.makedirs(args.output_dir, exist_ok=True)
    df = pd.read_csv(args.csv)
    print(f"Loaded {len(df)} benchmark records from {args.csv}.")

    plot_budget_vs_fault_detection(df, args.output_dir)
    plot_efficiency_frontier(df, args.output_dir)
    plot_affected_coverage(df, args.output_dir)
    plot_selection_ratios(df, args.output_dir)
    print("All benchmark charts successfully generated.")

if __name__ == "__main__":
    main()
