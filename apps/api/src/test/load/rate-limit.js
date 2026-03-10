export const options = {
    vus: 1,
    iterations: 150, // more than your 100/min limit
    thresholds: {
      'checks': ['rate>0.60'],  // at least 60% should pass (100/150)
    },
};