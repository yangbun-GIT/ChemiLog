/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ["./index.html", "./src/**/*.{vue,js,ts,jsx,tsx}"],
  theme: {
    extend: {
      gridTemplateColumns: {
        35: "repeat(35, minmax(0, 1fr))",
        53: "repeat(53, minmax(0, 1fr))",
      },
    },
  },
  plugins: [],
};
