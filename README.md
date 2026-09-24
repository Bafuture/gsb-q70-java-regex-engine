# 自研正则表达式引擎

Pair-wise GSB 标注任务仓库（第 6 批 / 70）。

| 项目 | 内容 |
|------|------|
| 任务类型 | Feature 迭代 |
| 任务难度 | 困难 |
| 语言/框架 | Java 17, Maven, JUnit 5, AssertJ |
| 环境可复现等级 | 无外部依赖 |
| 构建方式 | Maven（含 mvnw wrapper，无需本机安装 Maven） |

## 运行方式

```bash
mvn -q verify        # 或 ./mvnw -q verify
```

## 背景

业务人员自己编写规则，JDK 自带的 `java.util.regex` 是回溯式引擎，
`(a+)+b`、`(a|a)*b` 这类模式在长输入上会发生灾难性回溯（指数复杂度），
可能把 CPU 打满。本引擎从零实现，**不调用 `java.util.regex`**，采用
Thompson 构造把正则编译为 ε-NFA，再用状态集模拟执行完成匹配，
对任何模式匹配耗时都是 `O(状态数 × 输入长度)`，从原理上免疫灾难性回溯。

## 使用示例

```java
import com.example.gsb.regex.Regex;
import com.example.gsb.regex.RegexOptions;

Regex.Pattern pattern = Regex.compile("(a+)+b");

boolean whole = pattern.matches("aaab");             // 整串匹配
pattern.find("xxaaabyy").ifPresent(m -> {
    int start = m.start();   // 2
    int end   = m.end();     // 6（半开区间 [start, end)）
    String s  = m.group();   // "aaab"
});

// 自定义资源上限（编译期状态数 / 匹配期输入长度）
Regex.Pattern guarded = Regex.compile("a*",
        new RegexOptions(10_000, 1_000_000));
```

## 支持的语法

| 特性 | 示例 | 说明 |
|------|------|------|
| 字面量 | `abc` | 普通字符原样匹配 |
| 任意字符 | `.` | 匹配任意字符（**包含换行符**） |
| 字符类 | `[abc]` | 命中列举的任意字符 |
| 区间 | `[a-z0-9_]` | 一个类中可写多个区间 |
| 取反字符类 | `[^0-9]` | 命中不在类中的字符 |
| 量词 | `a*` `a+` `a?` | 贪婪性对模拟执行无影响（见“已知限制”） |
| 分组 | `(ab)+` | 支持嵌套；分组不产生捕获组 |
| 交替 | `a\|b\|c`、`(ab\|cd)e` | 多分支 |
| 空表达式 | `a\|`、`()` | 匹配空串，可被量词修饰（如 `(a*)*`） |
| 元字符转义 | `\*` `\.` `\[` `\\` 等 | 反斜杠后字符按字面量处理 |
| 控制字符转义 | `\n` `\t` `\r` `\f` | 字符类内外均可用 |
| 预定义字符类 | `\d` `\w` `\s` 及大写取反 `\D` `\W` `\S` | 字符类内外均可用（类内不支持大写取反） |

## 错误与上限（满足业务配置的可观测性要求）

- 编译期语法错误抛 `RegexSyntaxException`，消息含 **0 基精确位置与原因**，
  并可通过 `position()` 取位置：
  - `(` 不配对：报告该左括号位置，如 `(a` → 位置 0；
  - 多余 `)`：报告右括号自身位置，如 `abc)` → 位置 3；
  - 量词无目标（开头、`(` 后、`|` 后）：报告量词位置；
  - 连续量词（如 `a**`）：报告第二个量词位置；
  - `[` 未闭合：报告 `[` 位置；空字符类 `[]` 报“字符类为空”；
  - 反向区间 `[z-a]` 报“起点大于终点”；末尾孤立 `\` 也会报错。
- 资源上限超限抛 `RegexLimitExceededException`（`RegexOptions` 可配置）：
  - `maxStates`（默认 10000）：编译期 NFA 状态数上限；
  - `maxInputLength`（默认 1000000）：匹配期输入字符数上限。

## 匹配语义

- `matches`：整串必须被模式完整消费（等价 `^pattern$`）。
- `find`：返回**第一个**匹配，采用“最左优先，同起点取最长”
  （leftmost-longest）；`Match` 的 `end` 为半开区间端点。
  可匹配空串的模式（如 `a*`）在无实质匹配时返回位置 0 的空匹配。

## 实现结构

| 文件 | 职责 |
|------|------|
| `Regex.java` | 公开入口：`compile / Pattern.matches / Pattern.find` |
| `Parser.java` + `Node.java` | 递归下降解析，正则文本 → AST |
| `CharClass.java` | 字符区间谓词（含 `.`、`\d\w\s`） |
| `NfaCompiler.java` + `Nfa.java` | Thompson 构造，AST → ε-NFA |
| `NfaMatcher.java` | 状态集模拟（ε-闭包 + 逐字符推进），无回溯 |
| `RegexSyntaxException` / `RegexLimitExceededException` | 错误类型 |

## 已知限制

- 不支持锚点 `^` `$` / `\b`、不支持环视、反向引用、捕获组
  （分组仅用于优先级与量词作用域）。
- 只有 `* + ?` 三种量词；不支持 `{n,m}` 区间量词，不支持非贪婪
  （`*?`）与占有量词（`*+`），连续量词会直接报语法错误。
- 量词统一按模拟执行语义处理：`matches` 下贪婪与否等价；`find` 固定为
  最左-最长语义，无法表达“最短匹配”。
- `.` 匹配包含 `\n` 在内的所有字符；没有 DOTALL 开关。
- 交替分支在编译时全部展开，超长分支/分组的模式会更早触及
  `maxStates` 上限（这是有意的资源保护，而非静默退化）。
- 字符基于 Java `char` 处理，不支持 Unicode 码点（代理对）层面的匹配。
