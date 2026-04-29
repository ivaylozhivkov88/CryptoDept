import os

def merge_android_files(file_list, output_file="CryptoDept_Full_Context.txt"):
    """
    Обединява съдържанието на всички файлове от проекта CryptoDept в един файл.
    """
    print(f"Стартиране на обединяването в {output_file}...")
    files_processed = 0
    
    with open(output_file, "w", encoding="utf-8") as outfile:
        for filepath in file_list:
            filepath = filepath.strip()
            
            if os.path.exists(filepath):
                files_processed += 1
                filename = os.path.basename(filepath)
                
                # Заглавна част за по-добра четимост от AI
                outfile.write(f"\n\n{'='*80}\n")
                outfile.write(f" PATH: {filepath}\n")
                outfile.write(f" FILE: {filename}\n")
                outfile.write(f"{'='*80}\n\n")
                
                try:
                    with open(filepath, "r", encoding="utf-8") as infile:
                        outfile.write(infile.read())
                    print(f"[{files_processed}] Успешно добавен: {filename}")
                except Exception as e:
                    outfile.write(f"[ГРЕШКА ПРИ ЧЕТЕНЕ НА {filename}: {e}]\n")
                    print(f"Грешка при: {filename}")
            else:
                print(f"Внимание: Файлът не е намерен: {filepath}")

    print(f"\n--- ГОТОВО! ---")
    print(f"Общо обработени файлове: {files_processed}")
    print(f"Резултатът е записан в: {os.path.abspath(output_file)}")

# Пълният списък с твоите файлове
my_files = [
    r"D:\CryptoDept\app\src\androidTest\java\com\cryptodept\ExampleInstrumentedTest.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\CryptoDeptApplication.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\MainActivity.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\data\api\AlphaVantageApi.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\data\api\BinanceFuturesApi.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\data\api\BinanceWebSocketManager.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\data\api\BlockchainApi.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\data\api\CoinbaseApi.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\data\api\CoinCapApi.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\data\api\CoinGeckoApi.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\data\api\CoinglassApi.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\data\api\CoinMarketCalApi.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\data\api\CoinPaprikaApi.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\data\api\CryptoNewsApi.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\data\api\CryptoPanicApi.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\data\api\EtherscanApi.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\data\api\FearGreedApi.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\data\api\KrakenApi.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\data\api\KrakenWebSocketManager.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\data\api\MultiSourcePriceAggregator.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\data\api\RssNewsParser.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\data\datastore\PreferencesManager.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\data\db\AlertDao.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\data\db\AlertEntity.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\data\db\CoinDao.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\data\db\CoinEntity.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\data\db\Converters.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\data\db\CryptoDatabase.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\data\db\PriceHistoryDao.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\data\db\PriceHistoryEntity.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\data\db\TradeJournalDao.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\data\db\TradeJournalEntity.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\data\remoteconfig\RemoteConfigManager.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\data\repository\AlertsRepositoryImpl.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\data\repository\AnalysisRepositoryImpl.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\data\repository\ChartRepositoryImpl.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\data\repository\CryptoRepositoryImpl.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\di\AppModule.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\di\DatabaseModule.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\di\FirebaseModule.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\di\NetworkModule.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\di\RepositoryModule.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\domain\model\AggregatedPrice.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\domain\model\Alert.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\domain\model\Coin.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\domain\model\CoinDetail.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\domain\model\CoinPrice.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\domain\model\CompositeSignal.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\domain\model\DerivativesData.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\domain\model\GlobalMarketData.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\domain\model\NetworkHealth.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\domain\model\NewsItem.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\domain\model\OHLCData.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\domain\model\PricePrediction.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\domain\model\RssNewsItem.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\domain\model\TechnicalIndicators.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\domain\repository\AlertsRepository.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\domain\repository\AnalysisRepository.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\domain\repository\ChartRepository.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\domain\repository\CryptoRepository.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\domain\repository\DerivativesRepository.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\domain\repository\MacroRepository.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\domain\repository\NewsRepository.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\domain\usecase\AddAlertUseCase.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\domain\usecase\ConfluenceAlertDetector.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\domain\usecase\DailyBriefingGenerator.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\domain\usecase\GetAlertsUseCase.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\domain\usecase\GetOHLCUseCase.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\domain\usecase\GetPricesUseCase.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\domain\usecase\GetRsiUseCase.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\domain\usecase\RefreshOHLCUseCase.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\domain\usecase\RefreshPricesUseCase.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\domain\usecase\RiskScoreEngine.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\domain\usecase\TechnicalAnalysisEngine.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\domain\usecase\prediction\ElliottWavePredictor.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\domain\usecase\prediction\FourierCyclePredictor.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\domain\usecase\prediction\FractalDimensionAnalyzer.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\domain\usecase\prediction\HurstExponentCalculator.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\domain\usecase\prediction\LinearRegressionPredictor.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\domain\usecase\prediction\MonteCarloPredictor.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\domain\usecase\prediction\PredictionEnsembleEngine.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\domain\usecase\prediction\WyckoffPhaseDetector.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\service\BootReceiver.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\service\CryptoMessagingService.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\service\CryptoPriceForegroundService.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\service\DailyBriefingWorker.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\service\PriceSyncWorker.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\service\SoundManager.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\ui\alerts\AlertsScreen.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\ui\analysis\AnalysisScreen.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\ui\boot\BootSequenceScreen.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\ui\briefing\DailyBriefingScreen.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\ui\calendar\CalendarScreen.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\ui\charts\ChartsScreen.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\ui\coindetail\CoinDetailScreen.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\ui\components\GlobalMarketBar.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\ui\components\PriceText.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\ui\components\TerminalBottomBar.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\ui\components\TerminalCommandBar.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\ui\components\TerminalComponents.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\ui\components\TickerTape.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\ui\components\crt\BootSequenceScreen.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\ui\components\crt\CRTOverlay.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\ui\dashboard\DashboardScreen.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\ui\derivatives\DerivativesScreen.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\ui\effects\GlitchEffect.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\ui\feargreed\FearGreedScreen.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\ui\indicators\IndicatorsScreen.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\ui\journal\TradeJournalScreen.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\ui\macro\MacroScreen.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\ui\markets\MarketsScreen.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\ui\navigation\NavGraph.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\ui\navigation\Screen.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\ui\news\NewsScreen.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\ui\news\NewsViewModel.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\ui\prediction\AnalysisLoadingScreen.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\ui\prediction\DeepAnalysisResultScreen.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\ui\prediction\PredictionScreen.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\ui\prediction\PredictionViewModel.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\ui\risk\RiskScoreScreen.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\ui\screensaver\MatrixRainScreen.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\ui\settings\SettingsScreen.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\ui\signals\SignalsScreen.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\ui\theme\Color.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\ui\theme\SoundProvider.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\ui\theme\Theme.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\ui\theme\Type.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\ui\widget\CryptoDeptWidget.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\viewmodel\AlertsViewModel.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\viewmodel\AnalysisViewModel.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\viewmodel\BriefingViewModel.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\viewmodel\CalendarViewModel.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\viewmodel\ChartsViewModel.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\viewmodel\CoinDetailViewModel.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\viewmodel\DashboardViewModel.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\viewmodel\DerivativesViewModel.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\viewmodel\FearGreedViewModel.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\viewmodel\GlobalMarketViewModel.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\viewmodel\IndicatorsViewModel.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\viewmodel\JournalViewModel.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\viewmodel\MacroViewModel.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\viewmodel\MarketsViewModel.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\viewmodel\RiskViewModel.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\viewmodel\SettingsViewModel.kt",
    r"D:\CryptoDept\app\src\main\java\com\cryptodept\viewmodel\SignalsViewModel.kt",
    r"D:\CryptoDept\app\src\test\java\com\cryptodept\ExampleUnitTest.kt"
]

if __name__ == "__main__":
    merge_android_files(my_files)