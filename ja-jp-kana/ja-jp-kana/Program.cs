using System.Text.Json;

var hiraganaJson = File.ReadAllText("../../../hiragana.json");
var katakanaJson = File.ReadAllText("../../../katakana.json");

var hiragana = JsonSerializer.Deserialize<List<Kana>>(hiraganaJson);
var katakana = JsonSerializer.Deserialize<List<Kana>>(katakanaJson);
var configPath = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), "kana/config.json");
if (!File.Exists(configPath))
{
    Directory.CreateDirectory(configPath.Replace("/config.json", ""));
    File.WriteAllText(configPath, JsonSerializer.Serialize(new Config()));
}
var config = JsonSerializer.Deserialize<Config>(File.ReadAllText(configPath));

var hasConfigured = false;

Console.Clear();
while (!hasConfigured)
{
    Console.WriteLine("=== Configuration ===");
    Console.WriteLine($"Allow (D)akuon (Currently {config.dakuon})");
    Console.WriteLine($"Allow (H)andakuon (Currently {config.handakuon})");
    Console.WriteLine($"Allow (Y)ouon (Currently {config.youon})");
    Console.WriteLine($"(M)ode (Currently {config.mode})");
    Console.WriteLine("(S)tart");
    var command = Console.ReadKey();
    Console.Clear();
    switch (command.KeyChar.ToString().ToLower())
    {
        case "d":
            config.dakuon = !config.dakuon;
            break;
        case "h":
            config.handakuon = !config.handakuon;
            break;
        case "y":
            config.youon = !config.youon;
            break;
        case "s":
            hasConfigured = true;
            break;
        case "m":
            config.mode = config.mode switch
            {
                KanaType.Hiragana => KanaType.Katakana,
                KanaType.Katakana => KanaType.All,
                KanaType.All => KanaType.Hiragana,
                _ => config.mode
            };
            break;
        default:
            Console.WriteLine($"Unknown Option '{command.KeyChar.ToString().ToLower()}'!");
            break;
    }
}
File.WriteAllText(configPath, JsonSerializer.Serialize(config));

var allKana = config.mode switch
{
    KanaType.Hiragana => hiragana,
    KanaType.Katakana => katakana,
    KanaType.All => [.. hiragana, .. katakana],
    _ => hiragana
};

var filteredKana = allKana.Where(kana => filter(kana, config)).Shuffle();

foreach (var kana in filteredKana)
{
    guessLoopRoumaji(kana);
}

Console.WriteLine("You got them all correct, congratulations!");

bool filter(Kana kana, Config config)
{
    if (!config.dakuon && kana.type.ToLower().Equals("dakuon"))
    {
        return false;
    } else if (!config.handakuon && kana.type.ToLower().Equals("handakuon"))
    {
        return false;
    } else if (!config.youon && kana.type.ToLower().Equals("youon"))
    {
        return false;
    }
    else
    {
        return true;
    }
}

void guessLoopRoumaji(Kana kana)
{
    Console.WriteLine($"What is the roumaji for {kana.kana}? (type={kana.type})");
    var guess = Console.ReadLine();
    if (guess.ToLower().Equals(kana.roumaji.ToLower()))
    {
        Console.WriteLine($"Correct, {kana.kana} is {kana.roumaji.ToLower()}");
        Console.WriteLine("Press any key to continue...");
        Console.ReadKey();
        Console.Clear();
    }
    else
    {
        Console.WriteLine($"Incorrect, {kana.kana} is {kana.roumaji.ToLower()}, not {guess.ToLower()}");
        Console.WriteLine("Press any key to continue...");
        Console.ReadKey();
        Console.Clear();
        Environment.Exit(0);
    }
}

public class Kana
{
    public string kana { get; set; }
    public string roumaji { get; set; }
    public string type { get; set; }
}

public class Config
{
    public bool dakuon { get; set; }
    public bool handakuon { get; set; }
    public bool youon { get; set; }
    public KanaType mode { get; set; }
}

public enum KanaType
{
    Hiragana,
    Katakana,
    // TODO: decide if N5 kanji should be an option
    N5Kanji, // reserved, unused for now, see above
    HiraganaKatakana, // reserved, unused for now, see above
    KatakanaN5Kanji, // reserved, unused for now, see above
    N5KanjiHiragana, // reserved, unused for now, see above
    All,
}