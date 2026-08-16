using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;

// AutoLandscapeOverlay.cs
// MonoBehaviour that forces landscape orientation on Android/Unity and
// shows an on-screen overlay with status, percent, and recent logs.

public class AutoLandscapeOverlay : MonoBehaviour
{
    [Header("UI References")]
    public Text statusText;     // сверху: "что сейчас идёт"
    public Text percentText;    // проценты
    public Text logsText;       // последние строки лога
    public int maxLogLines = 10;

    Queue<string> logs = new Queue<string>();

    void OnEnable()
    {
        Application.logMessageReceived += HandleLog;
    }

    void OnDisable()
    {
        Application.logMessageReceived -= HandleLog;
    }

    void HandleLog(string logString, string stackTrace, LogType type)
    {
        string line = $"[{type}] {logString}";
        logs.Enqueue(line);
        while (logs.Count > maxLogLines) logs.Dequeue();
        UpdateLogsText();
    }

    void UpdateLogsText()
    {
        if (logsText != null)
            logsText.text = string.Join("\n", logs.ToArray());
    }

    void UpdatePercentText(float p)
    {
        if (percentText != null)
            percentText.text = $"{Mathf.RoundToInt(p * 100f)}%";
    }

    IEnumerator SimulateProgress() // пример: убрать/заменить на реальную логику
    {
        float t = 0f;
        while (t < 1f)
        {
            t += Time.deltaTime * 0.2f;
            UpdatePercentText(t);
            yield return null;
        }
        UpdatePercentText(1f);
    }

    public void ShowOverlay(string initialStatus = "Запускается...")
    {
        gameObject.SetActive(true);
        if (statusText != null) statusText.text = initialStatus;
    }

    public void HideOverlay()
    {
        gameObject.SetActive(false);
    }

    // Вызывать сразу после выбора Among Us
    public void OnAmongUsSelected()
    {
        // 1) Переключаем ориентацию Unity
        Screen.orientation = ScreenOrientation.LandscapeLeft;

        // 2) На Android дополнительно запрашиваем у Activity ландшафт (через JNI)
#if UNITY_ANDROID && !UNITY_EDITOR
        TryForceAndroidLandscape();
#endif

        // 3) Включаем оверлей и задаём статус
        ShowOverlay("Подготовка...");

        // 4) Запускаем обновление процентов (замените на реальную логику)
        StartCoroutine(SimulateProgress());
    }

#if UNITY_ANDROID && !UNITY_EDITOR
    void TryForceAndroidLandscape()
    {
        try
        {
            using (var unityPlayer = new AndroidJavaClass("com.unity3d.player.UnityPlayer"))
            {
                var activity = unityPlayer.GetStatic<AndroidJavaObject>("currentActivity");
                using (var activityInfo = new AndroidJavaClass("android.content.pm.ActivityInfo"))
                {
                    int landscape = activityInfo.GetStatic<int>("SCREEN_ORIENTATION_LANDSCAPE");
                    activity.Call("setRequestedOrientation", landscape);
                }
            }
        }
        catch (System.Exception e)
        {
            Debug.LogWarning("Не удалось запросить ориентацию Activity: " + e);
        }
    }
#endif
}
