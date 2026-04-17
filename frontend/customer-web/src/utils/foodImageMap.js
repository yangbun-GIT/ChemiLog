// frontend/customer-web/src/utils/foodImageMap.js
const KEYWORD_IMAGE_MAP = [
  { keyword: "삼겹살", url: "https://images.pexels.com/photos/5779368/pexels-photo-5779368.jpeg?auto=compress&cs=tinysrgb&w=1200" },
  { keyword: "곱창", url: "https://images.pexels.com/photos/1453767/pexels-photo-1453767.jpeg?auto=compress&cs=tinysrgb&w=1200" },
  { keyword: "된장찌개", url: "https://images.pexels.com/photos/4518838/pexels-photo-4518838.jpeg?auto=compress&cs=tinysrgb&w=1200" },
  { keyword: "김치찌개", url: "https://images.pexels.com/photos/4198019/pexels-photo-4198019.jpeg?auto=compress&cs=tinysrgb&w=1200" },
  { keyword: "비빔밥", url: "https://images.pexels.com/photos/723198/pexels-photo-723198.jpeg?auto=compress&cs=tinysrgb&w=1200" },
  { keyword: "회", url: "https://images.pexels.com/photos/3296279/pexels-photo-3296279.jpeg?auto=compress&cs=tinysrgb&w=1200" },
  { keyword: "초밥", url: "https://images.pexels.com/photos/357756/pexels-photo-357756.jpeg?auto=compress&cs=tinysrgb&w=1200" },
  { keyword: "라면", url: "https://images.pexels.com/photos/884600/pexels-photo-884600.jpeg?auto=compress&cs=tinysrgb&w=1200" },
  { keyword: "치킨", url: "https://images.pexels.com/photos/2338407/pexels-photo-2338407.jpeg?auto=compress&cs=tinysrgb&w=1200" },
  { keyword: "피자", url: "https://images.pexels.com/photos/825661/pexels-photo-825661.jpeg?auto=compress&cs=tinysrgb&w=1200" },
  { keyword: "햄버거", url: "https://images.pexels.com/photos/1639562/pexels-photo-1639562.jpeg?auto=compress&cs=tinysrgb&w=1200" },
  { keyword: "샐러드", url: "https://images.pexels.com/photos/257816/pexels-photo-257816.jpeg?auto=compress&cs=tinysrgb&w=1200" },
  { keyword: "요거트", url: "https://images.pexels.com/photos/704569/pexels-photo-704569.jpeg?auto=compress&cs=tinysrgb&w=1200" },
  { keyword: "아이스크림", url: "https://images.pexels.com/photos/1352278/pexels-photo-1352278.jpeg?auto=compress&cs=tinysrgb&w=1200" },
  { keyword: "콜라", url: "https://images.pexels.com/photos/2983101/pexels-photo-2983101.jpeg?auto=compress&cs=tinysrgb&w=1200" },
  { keyword: "커피", url: "https://images.pexels.com/photos/302899/pexels-photo-302899.jpeg?auto=compress&cs=tinysrgb&w=1200" },
  { keyword: "돈까스", url: "https://images.pexels.com/photos/2098085/pexels-photo-2098085.jpeg?auto=compress&cs=tinysrgb&w=1200" },
  { keyword: "제육", url: "https://images.pexels.com/photos/958545/pexels-photo-958545.jpeg?auto=compress&cs=tinysrgb&w=1200" },
];

const CATEGORY_FALLBACK_MAP = {
  한식: "https://images.pexels.com/photos/2098130/pexels-photo-2098130.jpeg?auto=compress&cs=tinysrgb&w=1200",
  중식: "https://images.pexels.com/photos/955137/pexels-photo-955137.jpeg?auto=compress&cs=tinysrgb&w=1200",
  일식: "https://images.pexels.com/photos/2098085/pexels-photo-2098085.jpeg?auto=compress&cs=tinysrgb&w=1200",
  건강식: "https://images.pexels.com/photos/1640774/pexels-photo-1640774.jpeg?auto=compress&cs=tinysrgb&w=1200",
  패스트푸드: "https://images.pexels.com/photos/2983101/pexels-photo-2983101.jpeg?auto=compress&cs=tinysrgb&w=1200",
  간편식: "https://images.pexels.com/photos/5638732/pexels-photo-5638732.jpeg?auto=compress&cs=tinysrgb&w=1200",
  디저트: "https://images.pexels.com/photos/291528/pexels-photo-291528.jpeg?auto=compress&cs=tinysrgb&w=1200",
  음료: "https://images.pexels.com/photos/2615323/pexels-photo-2615323.jpeg?auto=compress&cs=tinysrgb&w=1200",
  면류: "https://images.pexels.com/photos/884600/pexels-photo-884600.jpeg?auto=compress&cs=tinysrgb&w=1200",
};

export function resolveFoodImageUrl(food) {
  const explicit = String(food?.imageUrl || "").trim();
  if (explicit) {
    return explicit;
  }

  const name = String(food?.name || "").toLowerCase();
  const matched = KEYWORD_IMAGE_MAP.find((entry) => name.includes(entry.keyword.toLowerCase()));
  if (matched) {
    return matched.url;
  }

  const category = String(food?.category || "");
  if (CATEGORY_FALLBACK_MAP[category]) {
    return CATEGORY_FALLBACK_MAP[category];
  }

  return "https://images.pexels.com/photos/1640774/pexels-photo-1640774.jpeg?auto=compress&cs=tinysrgb&w=1200";
}
