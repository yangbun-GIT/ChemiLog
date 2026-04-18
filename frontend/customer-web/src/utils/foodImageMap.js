// frontend/customer-web/src/utils/foodImageMap.js
const EXACT_IMAGE_MAP = {
  "삼겹살 구이 1인분": "https://source.unsplash.com/1200x800/?samgyeopsal,korean-bbq",
  "곱창구이 1인분": "https://source.unsplash.com/1200x800/?grilled-offal,korean-food",
  "된장찌개 1인분": "https://source.unsplash.com/1200x800/?doenjang-jjigae,korean-stew",
  "김치찌개 1인분": "https://source.unsplash.com/1200x800/?kimchi-jjigae,korean-stew",
  "비빔밥 1인분": "https://source.unsplash.com/1200x800/?bibimbap,korean-food",
  "회덮밥 1인분": "https://source.unsplash.com/1200x800/?sashimi-rice-bowl,japanese-food",
  "불닭볶음면": "https://source.unsplash.com/1200x800/?spicy-ramen,korean-food",
  "닭가슴살 스테이크": "https://source.unsplash.com/1200x800/?chicken-breast,healthy-meal",
  "연어 샐러드": "https://source.unsplash.com/1200x800/?salmon-salad,healthy-food",
  "아이스아메리카노": "https://source.unsplash.com/1200x800/?iced-americano,coffee",
  "초코 도넛 1개": "https://source.unsplash.com/1200x800/?chocolate-donut,dessert",
  "콜라 1캔": "https://source.unsplash.com/1200x800/?cola,drink",
};

const FOOD_IMAGE_KEYWORD_RULES = [
  {
    keywords: ["삼겹살", "제육", "불고기", "곱창", "닭갈비", "갈비"],
    url: "https://source.unsplash.com/1200x800/?korean-bbq,meat",
  },
  {
    keywords: ["된장찌개", "김치찌개", "순두부찌개", "갈비탕", "설렁탕", "찌개"],
    url: "https://source.unsplash.com/1200x800/?korean-stew,soup",
  },
  {
    keywords: ["비빔밥", "회덮밥", "볶음밥", "규동", "현미밥", "덮밥", "도시락", "삼각김밥"],
    url: "https://source.unsplash.com/1200x800/?rice-bowl,korean-food",
  },
  {
    keywords: ["라면", "불닭", "짜장면", "짬뽕", "우동", "냉면", "쌀국수", "면"],
    url: "https://source.unsplash.com/1200x800/?noodles,ramen",
  },
  {
    keywords: ["치즈버거", "불고기버거", "햄버거", "핫도그", "감자튀김", "치킨텐더"],
    url: "https://source.unsplash.com/1200x800/?burger,fast-food",
  },
  {
    keywords: ["프라이드치킨", "치킨"],
    url: "https://source.unsplash.com/1200x800/?fried-chicken",
  },
  {
    keywords: ["피자"],
    url: "https://source.unsplash.com/1200x800/?pizza",
  },
  {
    keywords: ["돈까스", "카츠"],
    url: "https://source.unsplash.com/1200x800/?tonkatsu,pork-cutlet",
  },
  {
    keywords: ["초밥", "회", "사시미"],
    url: "https://source.unsplash.com/1200x800/?sushi,sashimi",
  },
  {
    keywords: ["마라탕", "마라샹궈", "탕수육", "중식"],
    url: "https://source.unsplash.com/1200x800/?chinese-food",
  },
  {
    keywords: ["샐러드", "포케", "오트밀", "고구마", "닭가슴살"],
    url: "https://source.unsplash.com/1200x800/?healthy-meal,salad",
  },
  {
    keywords: ["요거트", "요구르트"],
    url: "https://source.unsplash.com/1200x800/?yogurt",
  },
  {
    keywords: ["아이스크림", "도넛", "티라미수", "치즈케이크", "아포가토", "마카롱", "케이크"],
    url: "https://source.unsplash.com/1200x800/?dessert,sweets",
  },
  {
    keywords: ["아메리카노", "밀크티", "콜라", "우유", "쉐이크", "주스", "에이드", "음료"],
    url: "https://source.unsplash.com/1200x800/?drink,beverage",
  },
  {
    keywords: ["샌드위치", "랩"],
    url: "https://source.unsplash.com/1200x800/?sandwich,wrap",
  },
  {
    keywords: ["프로틴", "단백질바", "단백질"],
    url: "https://source.unsplash.com/1200x800/?protein-bar,fitness-food",
  },
];

const CATEGORY_FALLBACK_MAP = {
  한식: "https://source.unsplash.com/1200x800/?korean-food",
  중식: "https://source.unsplash.com/1200x800/?chinese-food",
  일식: "https://source.unsplash.com/1200x800/?japanese-food",
  건강식: "https://source.unsplash.com/1200x800/?healthy-food",
  패스트푸드: "https://source.unsplash.com/1200x800/?fast-food",
  간편식: "https://source.unsplash.com/1200x800/?meal-prep,ready-meal",
  유제품: "https://source.unsplash.com/1200x800/?dairy-food",
  음료: "https://source.unsplash.com/1200x800/?beverage",
  면류: "https://source.unsplash.com/1200x800/?noodles",
  디저트: "https://source.unsplash.com/1200x800/?dessert",
  분식: "https://source.unsplash.com/1200x800/?street-food",
  간식: "https://source.unsplash.com/1200x800/?snack-food",
};

function isPlaceholderUrl(url) {
  const target = String(url || "").toLowerCase();
  if (!target) return true;
  return target.includes("loremflickr.com") || target.includes("/minio/images/");
}

function imageByFoodName(name) {
  const normalizedName = String(name || "").trim();
  if (!normalizedName) return "";

  if (EXACT_IMAGE_MAP[normalizedName]) {
    return EXACT_IMAGE_MAP[normalizedName];
  }

  for (const rule of FOOD_IMAGE_KEYWORD_RULES) {
    if (rule.keywords.some((keyword) => normalizedName.includes(keyword))) {
      return rule.url;
    }
  }
  return "";
}

export function resolveFoodImageUrl(food) {
  const explicit = String(food?.imageUrl || "").trim();
  if (explicit && !isPlaceholderUrl(explicit)) {
    return explicit;
  }

  const byName = imageByFoodName(food?.name);
  if (byName) {
    return byName;
  }

  const category = String(food?.category || "").trim();
  if (CATEGORY_FALLBACK_MAP[category]) {
    return CATEGORY_FALLBACK_MAP[category];
  }

  return "https://source.unsplash.com/1200x800/?healthy-food";
}
