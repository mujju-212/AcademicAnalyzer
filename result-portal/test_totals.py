import json

# Test weighted total calculation
weighted_totals = [94.13, 95.75, 94.35, 92.35]
total = sum(weighted_totals)

print(f"Individual weighted totals: {weighted_totals}")
print(f"Sum: {total}")
print(f"Expected from DB: 376.58")
print(f"Match: {abs(total - 376.58) < 0.01}")

# The weighted total IS the sum of subject weighted scores
# Each subject is out of 100, so 4 subjects = 400 total max
print(f"\nTotal max marks: {len(weighted_totals) * 100}")
print(f"Percentage: {(total / 400) * 100:.2f}%")
