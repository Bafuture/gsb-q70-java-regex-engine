# 自研正则表达式引擎

面向业务规则配置的轻量正则引擎。内部使用 **Thompson 构造**把模式编译为 NFA，
匹配时做**状态集模拟**（不调用 `java.util.regex`，不使用回溯），
因此 `(a+)+b` 这类模式不存在灾难性回溯（ReDoS），匹配耗时为 O(输入长度 × NFA 状态数)。

## 运行方式

```bash
./mvnw -q verify
```

## 快速上手

```java
import com.example.gsb.regex.Regex;
import com.example.gsb.regex.MatchResult;
import com.example.gsb.regex.RegexOptions;

// 编译（语法错误在此阶段抛出 RegexSyntaxException，含精确位置）
Regex re = Regex.compile("(a+)+b");

boolean ok = re.matches("aab");                 // 整串匹配
Optional<MatchResult> m = re.find("xxaabyy");   // 查找 -> MatchResult[start=2, end=5)

// 自定义安全上限：最多 1000 个 NFA 状态、输入最长 10 万字符
Regex limited = Regex.compile("[a-z]+", new RegexOptions(1000, 100_000));
```

## 支持的语法

| 语法 | 示例 | 说明 |
|------|------|------|
| 字面量 | `abc` | 普通字符按字面匹配 |
| 任意字符 | `a.c` | `.` 匹配任意单个字符（含换行） |
| 字符类 | `[abc]` | 枚举字符 |
| 字符区间 | `[a-z0-9]` | 闭区间，可多个混写 |
| 取反字符类 | `[^0-9]` | 匹配不在类中的字符 |
| 量词 | `*` `+` `?` | 作用于前一个原子（字符、字符类或分组） |
| 分组 | `(ab|cd)+` | 仅用于限定优先级，不做捕获 |
| 交替 | `cat|dog` | 支持空分支（`a|` 可匹配空串） |

## 匹配语义

- `matches(input)`：整串匹配，输入必须被模式完全消费。
- `find(input)`：返回**最左**匹配（起点相同时取**最长**，POSIX leftmost-longest 风格），
  结果为 `[start, end)` 半开区间；无匹配返回 `Optional.empty()`。

## 错误与上限

- **编译期语法错误**抛出 `RegexSyntaxException`，`getPosition()` 给出出错字符下标，
  消息同时包含原因与位置，覆盖：括号不配对（`Unclosed group` / `Unmatched ')'`）、
  量词位置非法（`has no target` / `follows another quantifier`）、
  字符类未闭合（`Unclosed character class`）、空字符类、非法区间（如 `[z-a]`）。
- **安全上限**通过 `RegexOptions(maxStates, maxInputLength)` 配置
  （默认 10,000 状态 / 1,000,000 字符），超限抛出 `RegexLimitExceededException`：
  状态数在编译期检查，输入长度在匹配期检查。

## 已知限制

- 不支持转义序列（`\d`、`\w`、`\.` 等），元字符无法按字面匹配。
- 不支持捕获组与反向引用，分组仅用于优先级；`find` 只返回整体匹配区间。
- 不支持锚点（`^` `$`）、懒惰量词（`*?`）、向前/向后查找。
- 不支持连续量词（`a**`、`a+?`），编译期报错。
- 字符类内不支持转义与嵌套，`[]` 视为空类报错；类末尾的 `-` 按字面处理。
- 按 `char`（UTF-16 代码单元）匹配，不处理 Unicode 码点与大小写折叠。
