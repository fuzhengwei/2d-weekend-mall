package cn.xiaofuge.mall.service;

import cn.xiaofuge.mall.domain.Product;
import java.math.BigDecimal;
import java.util.List;

final class MallData {
    private MallData() {
    }

    static List<Product> products() {
        return List.of(
                product("p001", "周末云朵沙发毯", "Restday", "居家", "🛋️", "像躺进周末里",
                        "加厚珊瑚绒双层锁温，180cm 宽幅，适合窝在沙发里看电影。", "129", "179", 38, 4.9,
                        List.of("保暖", "居家", "周末"), "周五晚上铺开，直接进入休息模式。",
                        "Restday 工坊", "18:00 下班 · 周末双休"),
                product("p002", "无班焦虑手冲壶", "SlowBrew", "咖啡", "🫖", "慢一点才有好味道",
                        "304 不锈钢细口壶，控流稳定，配周末手冲咖啡课程卡。", "219", "259", 22, 4.8,
                        List.of("咖啡", "治愈", "送礼"), "周六上午一杯，先不看工作群。",
                        "SlowBrew Studio", "17:30 下班 · 今晚不加班"),
                product("p003", "两天周末桌面香薰", "Quiet Air", "香薰", "🕯️", "把客厅变成森林",
                        "雪松、苔藓与柑橘前调，燃烧 45 小时，附灭火钟罩。", "159", "199", 45, 4.7,
                        List.of("香薰", "放松", "客厅"), "适合周日阅读和整理心情。",
                        "Quiet Air Lab", "18:00 下班 · 不开夜会"),
                product("p004", "轻旅行双肩包", "Weekender", "旅行", "🎒", "两天一夜刚好",
                        "22L 防泼水，独立鞋仓和 16 英寸电脑夹层，可放折叠伞。", "399", "499", 31, 4.8,
                        List.of("旅行", "通勤", "防泼水"), "周五下班直接出发。",
                        "Weekender Works", "17:30 下班 · 周五早走"),
                product("p005", "懒人周末无线耳机", "CozySound", "数码", "🎧", "把噪音调成静音",
                        "42dB 主动降噪，10 分钟快充听 3 小时，入耳记忆棉。", "549", "699", 26, 4.9,
                        List.of("降噪", "数码", "通勤"), "下午小睡时更安静。",
                        "CozySound Tech", "18:00 下班 · 周末双休"),
                product("p006", "不加班护眼台灯", "Sunlike", "家居", "💡", "夜读也不刺眼",
                        "AA 级照度，暖白双模式，支持 15 分钟休眠延时。", "279", "329", 40, 4.6,
                        List.of("阅读", "护眼", "卧室"), "读完一章就提醒你休息。",
                        "Sunlike Home", "18:00 下班 · 不加班文化"),
                product("p007", "周末电影投影仪", "Tiny Cinema", "影音", "🎬", "客厅秒变小影院",
                        "1080P 物理分辨率，自动梯形校正，内置周末片单推荐。", "1999", "2499", 15, 4.8,
                        List.of("投影", "影音", "大屏"), "和喜欢的人一起看一部老电影。",
                        "Tiny Cinema Studio", "18:00 下班 · 不开夜会"),
                product("p008", "两天无班马卡龙", "Sweet Off", "甜品", "🧁", "甜一点，压力少一点",
                        "低温鲜制 8 枚，含开心果、玫瑰荔枝和海盐巧克力。", "98", "128", 50, 4.9,
                        List.of("甜品", "下午茶", "鲜花"), "周六下午配红茶刚好。",
                        "Sweet Off Bakery", "17:30 下班 · 周末双休"),
                product("p009", "周末骑行保温杯", "Ride Day", "户外", "🚲", "骑着车去晒太阳",
                        "500ml 弹盖设计，6 小时保温，杯身防滑磨砂。", "139", "169", 58, 4.7,
                        List.of("户外", "骑行", "保温"), "城市绿道骑行的固定装备。",
                        "Ride Day Outdoor", "18:00 下班 · 周末双休"),
                product("p010", "不加班立式绿植", "Plant Break", "绿植", "🪴", "桌边多一口氧气",
                        "龟背竹带陶盆，缓释肥 90 天，附懒人浇水指南。", "89", "119", 65, 4.8,
                        List.of("绿植", "桌面", "自然"), "休息日擦叶子也很解压。",
                        "Plant Break Garden", "18:00 下班 · 不开夜会"),
                product("p011", "周末游戏手柄", "Play Cube", "游戏", "🎮", "赢一场就睡觉",
                        "霍尔摇杆，双马达震动，支持 PC 和手机双连接。", "329", "399", 34, 4.7,
                        List.of("游戏", "聚会", "无线"), "别贪多，两小时最快乐。",
                        "Play Cube Studio", "18:00 下班 · 周末双休"),
                product("p012", "周日晚安身体乳", "Sleep Well", "个护", "🧴", "把星期天调成睡眠模式",
                        "神经酰胺和薰衣草精油，快速吸收不黏腻。", "119", "149", 72, 4.9,
                        List.of("个护", "晚安", "薰衣草"), "周日晚上为自己按下暂停键。",
                        "Sleep Well Care", "18:00 下班 · 不加班文化")
        );
    }

    private static Product product(
            String id, String name, String brand, String category, String emoji, String tagline,
            String description, String price, String originalPrice, int stock, double rating,
            List<String> tags, String weekendTip, String companyName, String workPolicy
    ) {
        return new Product(id, name, brand, category, emoji, tagline, description,
                new BigDecimal(price), new BigDecimal(originalPrice), stock, rating, tags, weekendTip,
                companyName, workPolicy);
    }
}
