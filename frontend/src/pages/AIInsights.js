import React, { useState, useEffect, useCallback } from 'react';
import {
  getAISummary,
  getRecentAnalyses,
  getRecentInsights,
  analyzeTranscript,
  categorizeTicket,
  generateCustomerInsights,
} from '../services/api';
import AiDataHandlingNotice from '../components/AiDataHandlingNotice';

const colors = {
  primary: '#4F46E5',
  primaryLight: '#6366F1',
  primaryBg: '#EEF2FF',
  success: '#10B981',
  successBg: '#D1FAE5',
  warning: '#F59E0B',
  warningBg: '#FEF3C7',
  danger: '#EF4444',
  dangerBg: '#FEE2E2',
  mixed: '#8B5CF6',
  mixedBg: '#EDE9FE',
  gray50: '#F9FAFB',
  gray100: '#F3F4F6',
  gray200: '#E5E7EB',
  gray300: '#D1D5DB',
  gray400: '#9CA3AF',
  gray500: '#6B7280',
  gray700: '#374151',
  gray900: '#111827',
};

const sentimentColors = {
  POSITIVE: { bg: colors.successBg, fg: colors.success },
  NEUTRAL: { bg: colors.gray100, fg: colors.gray500 },
  NEGATIVE: { bg: colors.dangerBg, fg: colors.danger },
  MIXED: { bg: colors.mixedBg, fg: colors.mixed },
};

const priorityColors = {
  HIGH: { bg: colors.dangerBg, fg: colors.danger },
  CRITICAL: { bg: colors.dangerBg, fg: colors.danger },
  MEDIUM: { bg: colors.warningBg, fg: colors.warning },
  LOW: { bg: colors.successBg, fg: colors.success },
};

const riskColors = {
  HIGH: colors.danger,
  MEDIUM: colors.warning,
  LOW: colors.success,
};

const s = {
  page: {
    minHeight: '100vh',
  },
  header: {
    display: 'flex',
    alignItems: 'center',
    gap: 12,
    marginBottom: 32,
  },
  headerIcon: {
    fontSize: 28,
    color: colors.primary,
  },
  heading: {
    fontSize: 28,
    fontWeight: 800,
    color: colors.gray900,
    letterSpacing: '-0.5px',
  },
  statsGrid: {
    display: 'grid',
    gridTemplateColumns: 'repeat(4, 1fr)',
    gap: 20,
    marginBottom: 36,
  },
  statCard: {
    background: 'linear-gradient(135deg, #f8f7ff 0%, #ffffff 100%)',
    borderRadius: 16,
    padding: '24px 28px',
    boxShadow: '0 1px 3px rgba(0,0,0,0.06), 0 4px 12px rgba(79,70,229,0.04)',
    border: `1px solid ${colors.gray100}`,
    transition: 'transform 0.15s, box-shadow 0.15s',
  },
  statValue: {
    fontSize: 32,
    fontWeight: 800,
    marginBottom: 4,
    letterSpacing: '-0.5px',
  },
  statLabel: {
    fontSize: 13,
    color: colors.gray500,
    fontWeight: 600,
    textTransform: 'uppercase',
    letterSpacing: '0.5px',
  },
  statSub: {
    fontSize: 12,
    color: colors.gray400,
    marginTop: 8,
  },
  card: {
    background: '#fff',
    borderRadius: 16,
    padding: 28,
    boxShadow: '0 1px 3px rgba(0,0,0,0.06), 0 4px 12px rgba(0,0,0,0.03)',
    border: `1px solid ${colors.gray100}`,
    marginBottom: 28,
  },
  cardTitle: {
    fontSize: 18,
    fontWeight: 700,
    color: colors.gray900,
    marginBottom: 20,
    display: 'flex',
    alignItems: 'center',
    gap: 8,
  },
  cardTitleIcon: {
    fontSize: 18,
    opacity: 0.7,
  },
  textarea: {
    width: '100%',
    minHeight: 120,
    padding: '14px 16px',
    borderRadius: 10,
    border: `1.5px solid ${colors.gray200}`,
    fontSize: 14,
    fontFamily: 'inherit',
    resize: 'vertical',
    outline: 'none',
    transition: 'border-color 0.15s',
    boxSizing: 'border-box',
  },
  input: {
    padding: '10px 14px',
    borderRadius: 10,
    border: `1.5px solid ${colors.gray200}`,
    fontSize: 14,
    outline: 'none',
    width: 140,
    transition: 'border-color 0.15s',
  },
  btnPrimary: {
    background: colors.primary,
    color: '#fff',
    border: 'none',
    padding: '10px 24px',
    borderRadius: 10,
    fontSize: 14,
    fontWeight: 600,
    cursor: 'pointer',
    display: 'inline-flex',
    alignItems: 'center',
    gap: 8,
    transition: 'background 0.15s, transform 0.1s',
  },
  btnDisabled: {
    background: colors.gray300,
    cursor: 'not-allowed',
  },
  formRow: {
    display: 'flex',
    alignItems: 'center',
    gap: 12,
    marginTop: 14,
  },
  resultPanel: {
    marginTop: 20,
    padding: 20,
    borderRadius: 12,
    background: colors.gray50,
    border: `1px solid ${colors.gray200}`,
  },
  resultFlash: {
    animation: 'none',
  },
  badge: {
    display: 'inline-block',
    padding: '4px 12px',
    borderRadius: 20,
    fontSize: 12,
    fontWeight: 700,
    textTransform: 'uppercase',
    letterSpacing: '0.3px',
  },
  tagPill: {
    display: 'inline-block',
    padding: '3px 10px',
    borderRadius: 16,
    fontSize: 12,
    fontWeight: 500,
    background: colors.primaryBg,
    color: colors.primary,
    marginRight: 6,
    marginBottom: 6,
  },
  star: {
    fontSize: 18,
    letterSpacing: 2,
  },
  starFilled: {
    color: '#FBBF24',
  },
  starEmpty: {
    color: colors.gray300,
  },
  barContainer: {
    marginBottom: 12,
  },
  barLabel: {
    display: 'flex',
    justifyContent: 'space-between',
    fontSize: 13,
    fontWeight: 600,
    marginBottom: 4,
    color: colors.gray700,
  },
  barTrack: {
    height: 24,
    borderRadius: 12,
    background: colors.gray100,
    overflow: 'hidden',
    position: 'relative',
  },
  confidenceBarTrack: {
    height: 6,
    borderRadius: 3,
    background: colors.gray100,
    overflow: 'hidden',
    marginTop: 8,
  },
  table: {
    width: '100%',
    borderCollapse: 'collapse',
  },
  th: {
    textAlign: 'left',
    padding: '12px 16px',
    fontSize: 11,
    fontWeight: 700,
    color: colors.gray500,
    textTransform: 'uppercase',
    letterSpacing: '0.6px',
    borderBottom: `2px solid ${colors.gray200}`,
  },
  td: {
    padding: '14px 16px',
    fontSize: 14,
    color: colors.gray700,
    borderBottom: `1px solid ${colors.gray100}`,
  },
  insightCard: {
    padding: 20,
    borderRadius: 12,
    background: '#fff',
    border: `1px solid ${colors.gray200}`,
    marginBottom: 12,
  },
  errorText: {
    color: colors.danger,
    fontSize: 13,
    fontWeight: 500,
    marginTop: 8,
  },
  spinner: {
    display: 'inline-block',
    width: 16,
    height: 16,
    border: '2px solid rgba(255,255,255,0.3)',
    borderTopColor: '#fff',
    borderRadius: '50%',
    animation: 'spin 0.6s linear infinite',
  },
  emptyState: {
    color: colors.gray500,
    fontSize: 14,
    textAlign: 'center',
    padding: 32,
  },
  checkItem: {
    display: 'flex',
    alignItems: 'flex-start',
    gap: 8,
    padding: '6px 0',
    fontSize: 14,
    color: colors.gray700,
  },
  sectionLabel: {
    fontSize: 13,
    fontWeight: 700,
    color: colors.gray500,
    textTransform: 'uppercase',
    letterSpacing: '0.4px',
    marginBottom: 8,
    marginTop: 16,
  },
  riskDot: {
    display: 'inline-block',
    width: 10,
    height: 10,
    borderRadius: '50%',
    marginRight: 6,
    verticalAlign: 'middle',
  },
};

const spinnerKeyframes = `
@keyframes spin {
  to { transform: rotate(360deg); }
}
@keyframes flashGreen {
  0% { box-shadow: 0 0 0 0 rgba(16,185,129,0.4); }
  50% { box-shadow: 0 0 0 8px rgba(16,185,129,0); }
  100% { box-shadow: 0 0 0 0 rgba(16,185,129,0); }
}
`;

function Spinner() {
  return <span style={s.spinner} />;
}

function Stars({ rating, max = 5 }) {
  const filled = Math.round(rating || 0);
  return (
    <span style={s.star}>
      {Array.from({ length: max }, (_, i) => (
        <span key={i} style={i < filled ? s.starFilled : s.starEmpty}>
          {i < filled ? '★' : '☆'}
        </span>
      ))}
    </span>
  );
}

function SentimentBadge({ sentiment }) {
  const sc = sentimentColors[sentiment] || sentimentColors.NEUTRAL;
  return (
    <span style={{ ...s.badge, background: sc.bg, color: sc.fg }}>
      {sentiment || 'N/A'}
    </span>
  );
}

function PriorityBadge({ priority }) {
  const pc = priorityColors[priority] || { bg: colors.gray100, fg: colors.gray500 };
  return (
    <span style={{ ...s.badge, background: pc.bg, color: pc.fg }}>
      {priority || 'N/A'}
    </span>
  );
}

function RiskDot({ risk }) {
  const color = riskColors[risk] || colors.gray400;
  return (
    <span>
      <span style={{ ...s.riskDot, background: color }} />
      <span style={{ fontSize: 13, color, fontWeight: 600 }}>{risk || 'N/A'}</span>
    </span>
  );
}

function ConfidenceBar({ value }) {
  const pct = Math.min(100, Math.max(0, (value || 0) * 100));
  return (
    <div style={s.confidenceBarTrack}>
      <div
        style={{
          height: '100%',
          width: `${pct}%`,
          borderRadius: 3,
          background: `linear-gradient(90deg, ${colors.primary}, ${colors.primaryLight})`,
          transition: 'width 0.4s ease',
        }}
      />
    </div>
  );
}

function SentimentBar({ label, count, max, color }) {
  const pct = max > 0 ? (count / max) * 100 : 0;
  return (
    <div style={s.barContainer}>
      <div style={s.barLabel}>
        <span>{label}</span>
        <span>{count}</span>
      </div>
      <div style={s.barTrack}>
        <div
          style={{
            height: '100%',
            width: `${pct}%`,
            borderRadius: 12,
            background: color,
            transition: 'width 0.5s ease',
            minWidth: count > 0 ? 24 : 0,
          }}
        />
      </div>
    </div>
  );
}

export default function AIInsights() {
  const [summary, setSummary] = useState(null);
  const [recentAnalyses, setRecentAnalyses] = useState([]);
  const [recentInsights, setRecentInsights] = useState([]);
  const [pageLoading, setPageLoading] = useState(true);
  const [pageError, setPageError] = useState(null);

  const [transcript, setTranscript] = useState('');
  const [transcriptSessionId, setTranscriptSessionId] = useState('');
  const [analyzeLoading, setAnalyzeLoading] = useState(false);
  const [analyzeError, setAnalyzeError] = useState(null);
  const [analyzeResult, setAnalyzeResult] = useState(null);
  const [analyzeFlash, setAnalyzeFlash] = useState(false);

  const [ticketId, setTicketId] = useState('');
  const [catLoading, setCatLoading] = useState(false);
  const [catError, setCatError] = useState(null);
  const [catResult, setCatResult] = useState(null);
  const [catFlash, setCatFlash] = useState(false);

  const [customerId, setCustomerId] = useState('');
  const [insightsLoading, setInsightsLoading] = useState(false);
  const [insightsError, setInsightsError] = useState(null);
  const [insightsResult, setInsightsResult] = useState(null);
  const [insightsFlash, setInsightsFlash] = useState(false);

  const loadPageData = useCallback(async () => {
    try {
      setPageLoading(true);
      setPageError(null);
      const [summaryRes, analysesRes, insightsRes] = await Promise.all([
        getAISummary(),
        getRecentAnalyses(),
        getRecentInsights(),
      ]);
      setSummary(summaryRes.data);
      setRecentAnalyses(analysesRes.data || []);
      setRecentInsights(insightsRes.data || []);
    } catch {
      setPageError('Failed to load AI insights data');
    } finally {
      setPageLoading(false);
    }
  }, []);

  useEffect(() => {
    loadPageData();
  }, [loadPageData]);

  const triggerFlash = (setter) => {
    setter(true);
    setTimeout(() => setter(false), 800);
  };

  const handleAnalyze = async () => {
    if (!transcript.trim()) return;
    try {
      setAnalyzeLoading(true);
      setAnalyzeError(null);
      setAnalyzeResult(null);
      const res = await analyzeTranscript({
        transcript: transcript.trim(),
        sessionId: transcriptSessionId ? Number(transcriptSessionId) : undefined,
      });
      setAnalyzeResult(res.data);
      triggerFlash(setAnalyzeFlash);
    } catch (err) {
      setAnalyzeError(err.response?.data?.message || 'Analysis failed');
    } finally {
      setAnalyzeLoading(false);
    }
  };

  const handleCategorize = async () => {
    if (!ticketId) return;
    try {
      setCatLoading(true);
      setCatError(null);
      setCatResult(null);
      const res = await categorizeTicket(Number(ticketId));
      setCatResult(res.data);
      triggerFlash(setCatFlash);
    } catch (err) {
      setCatError(err.response?.data?.message || 'Categorization failed');
    } finally {
      setCatLoading(false);
    }
  };

  const handleGenerateInsights = async () => {
    if (!customerId) return;
    try {
      setInsightsLoading(true);
      setInsightsError(null);
      setInsightsResult(null);
      const res = await generateCustomerInsights(Number(customerId));
      setInsightsResult(res.data);
      triggerFlash(setInsightsFlash);
    } catch (err) {
      setInsightsError(err.response?.data?.message || 'Insight generation failed');
    } finally {
      setInsightsLoading(false);
    }
  };

  const sentimentDist = summary?.sentimentDistribution || {};
  const maxSentiment = Math.max(
    sentimentDist.POSITIVE || 0,
    sentimentDist.NEUTRAL || 0,
    sentimentDist.NEGATIVE || 0,
    sentimentDist.MIXED || 0,
    1
  );

  const sentimentScore = summary?.overallSentimentScore ?? 0;
  const overallSentimentLabel = sentimentScore > 0.2 ? 'POSITIVE' : sentimentScore < -0.2 ? 'NEGATIVE' : 'NEUTRAL';
  const sentimentValueColor =
    sentimentScore > 0.2
      ? colors.success
      : sentimentScore < -0.2
        ? colors.danger
        : colors.warning;

  if (pageLoading) {
    return <div style={s.emptyState}>Loading AI Insights...</div>;
  }

  if (pageError) {
    return <div style={{ ...s.emptyState, color: colors.danger }}>{pageError}</div>;
  }

  const escalation = summary?.escalationRiskBreakdown || {};

  return (
    <div style={s.page}>
      <style>{spinnerKeyframes}</style>

      {/* Header */}
      <div style={s.header}>
        <span style={s.headerIcon}>✦</span>
        <h1 style={s.heading}>AI Insights</h1>
      </div>

      <div style={{ marginBottom: 28 }}>
        <AiDataHandlingNotice />
      </div>

      {/* Summary Stats */}
      <div style={s.statsGrid}>
        <div style={s.statCard}>
          <div style={{ ...s.statValue, color: sentimentValueColor }}>
            {overallSentimentLabel}
          </div>
          <div style={s.statLabel}>Overall Sentiment</div>
          <div style={s.statSub}>
            Score: {summary?.overallSentimentScore?.toFixed(2) ?? '—'}
          </div>
        </div>

        <div style={s.statCard}>
          <div style={{ ...s.statValue, color: colors.warning }}>
            {summary?.averageSatisfactionScore?.toFixed(1) ?? '—'}
            <span style={{ fontSize: 16, fontWeight: 500, color: colors.gray400 }}> / 5</span>
          </div>
          <div style={s.statLabel}>Avg Satisfaction</div>
          <div style={{ marginTop: 8 }}>
            <Stars rating={summary?.averageSatisfactionScore || 0} />
          </div>
        </div>

        <div style={s.statCard}>
          <div style={{ ...s.statValue, color: colors.primary }}>
            {summary?.totalAnalyses ?? 0}
          </div>
          <div style={s.statLabel}>Total Analyses</div>
          <div style={s.statSub}>Completed to date</div>
        </div>

        <div style={s.statCard}>
          <div style={{ display: 'flex', alignItems: 'baseline', gap: 12 }}>
            <span style={{ ...s.statValue, color: colors.danger }}>
              {escalation.high || 0}
            </span>
            <span style={{ fontSize: 14, color: colors.gray500, fontWeight: 500 }}>high risk</span>
          </div>
          <div style={s.statLabel}>Escalation Risk</div>
          <div style={s.statSub}>
            Med: {escalation.medium || 0} · Low: {escalation.low || 0}
          </div>
        </div>
      </div>

      {/* Sentiment Distribution */}
      <div style={s.card}>
        <div style={s.cardTitle}>
          <span style={s.cardTitleIcon}>📊</span>
          Sentiment Distribution
        </div>
        <SentimentBar label="Positive" count={sentimentDist.POSITIVE || 0} max={maxSentiment} color={colors.success} />
        <SentimentBar label="Neutral" count={sentimentDist.NEUTRAL || 0} max={maxSentiment} color={colors.gray400} />
        <SentimentBar label="Negative" count={sentimentDist.NEGATIVE || 0} max={maxSentiment} color={colors.danger} />
        <SentimentBar label="Mixed" count={sentimentDist.MIXED || 0} max={maxSentiment} color={colors.mixed} />
      </div>

      {/* Analyze Transcript */}
      <div style={s.card}>
        <div style={s.cardTitle}>
          <span style={s.cardTitleIcon}>🔍</span>
          Analyze Transcript
        </div>
        <textarea
          style={s.textarea}
          placeholder="Paste a support transcript here..."
          value={transcript}
          onChange={(e) => setTranscript(e.target.value)}
          onFocus={(e) => { e.target.style.borderColor = colors.primary; }}
          onBlur={(e) => { e.target.style.borderColor = colors.gray200; }}
        />
        <div style={s.formRow}>
          <input
            type="number"
            style={s.input}
            placeholder="Session ID"
            value={transcriptSessionId}
            onChange={(e) => setTranscriptSessionId(e.target.value)}
            onFocus={(e) => { e.target.style.borderColor = colors.primary; }}
            onBlur={(e) => { e.target.style.borderColor = colors.gray200; }}
          />
          <button
            style={{
              ...s.btnPrimary,
              ...(analyzeLoading || !transcript.trim() ? s.btnDisabled : {}),
            }}
            disabled={analyzeLoading || !transcript.trim()}
            onClick={handleAnalyze}
          >
            {analyzeLoading ? <><Spinner /> Analyzing...</> : 'Analyze'}
          </button>
        </div>
        {analyzeError && <div style={s.errorText}>{analyzeError}</div>}
        {analyzeResult && (
          <div
            style={{
              ...s.resultPanel,
              ...(analyzeFlash
                ? { animation: 'flashGreen 0.8s ease' }
                : {}),
            }}
          >
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: 12, alignItems: 'center', marginBottom: 16 }}>
              <SentimentBadge sentiment={analyzeResult.sentimentType} />
              <span style={{ ...s.badge, background: colors.primaryBg, color: colors.primary }}>
                Score: {analyzeResult.sentimentScore?.toFixed(2) ?? '—'}
              </span>
              <span style={{ ...s.badge, background: colors.gray100, color: colors.gray700 }}>
                Resolution: {analyzeResult.resolutionQuality || 'N/A'}
              </span>
              <RiskDot risk={analyzeResult.escalationRisk} />
            </div>

            <div style={s.sectionLabel}>Summary</div>
            <p style={{ fontSize: 14, color: colors.gray700, lineHeight: 1.6, margin: '0 0 12px' }}>
              {analyzeResult.summary}
            </p>

            <div style={s.sectionLabel}>Key Topics</div>
            <div style={{ marginBottom: 12 }}>
              {(analyzeResult.keyTopics || []).map((topic, i) => (
                <span key={i} style={s.tagPill}>{topic}</span>
              ))}
              {(!analyzeResult.keyTopics || analyzeResult.keyTopics.length === 0) && (
                <span style={{ fontSize: 13, color: colors.gray400 }}>None identified</span>
              )}
            </div>

            <div style={s.sectionLabel}>Satisfaction</div>
            <div style={{ marginBottom: 12 }}>
              <Stars rating={analyzeResult.customerSatisfactionScore || 0} />
              <span style={{ marginLeft: 8, fontSize: 14, color: colors.gray500 }}>
                {analyzeResult.customerSatisfactionScore?.toFixed(1) ?? '—'} / 5
              </span>
            </div>

            {analyzeResult.suggestedFollowUps && analyzeResult.suggestedFollowUps.length > 0 && (
              <>
                <div style={s.sectionLabel}>Suggested Follow-ups</div>
                {analyzeResult.suggestedFollowUps.map((item, i) => (
                  <div key={i} style={s.checkItem}>
                    <span style={{ color: colors.primary, fontSize: 16, lineHeight: 1 }}>☐</span>
                    <span>{item}</span>
                  </div>
                ))}
              </>
            )}
          </div>
        )}
      </div>

      {/* Categorize Ticket */}
      <div style={s.card}>
        <div style={s.cardTitle}>
          <span style={s.cardTitleIcon}>🏷️</span>
          Categorize Ticket
        </div>
        <div style={s.formRow}>
          <input
            type="number"
            style={s.input}
            placeholder="Ticket ID"
            value={ticketId}
            onChange={(e) => setTicketId(e.target.value)}
            onFocus={(e) => { e.target.style.borderColor = colors.primary; }}
            onBlur={(e) => { e.target.style.borderColor = colors.gray200; }}
          />
          <button
            style={{
              ...s.btnPrimary,
              ...(catLoading || !ticketId ? s.btnDisabled : {}),
            }}
            disabled={catLoading || !ticketId}
            onClick={handleCategorize}
          >
            {catLoading ? <><Spinner /> Categorizing...</> : 'Categorize'}
          </button>
        </div>
        {catError && <div style={s.errorText}>{catError}</div>}
        {catResult && (
          <div
            style={{
              ...s.resultPanel,
              ...(catFlash ? { animation: 'flashGreen 0.8s ease' } : {}),
            }}
          >
            <div style={{ display: 'flex', gap: 12, alignItems: 'center', marginBottom: 12 }}>
              <span style={{ ...s.badge, background: colors.primaryBg, color: colors.primary }}>
                {catResult.suggestedCategory || 'Uncategorized'}
              </span>
              <PriorityBadge priority={catResult.suggestedPriority} />
            </div>
            <div style={s.sectionLabel}>Reasoning</div>
            <p style={{ fontSize: 14, color: colors.gray700, lineHeight: 1.6, margin: 0 }}>
              {catResult.reasoning || 'No reasoning provided.'}
            </p>
          </div>
        )}
      </div>

      {/* Generate Customer Insights */}
      <div style={s.card}>
        <div style={s.cardTitle}>
          <span style={s.cardTitleIcon}>💡</span>
          Generate Customer Insights
        </div>
        <div style={s.formRow}>
          <input
            type="number"
            style={s.input}
            placeholder="Customer ID"
            value={customerId}
            onChange={(e) => setCustomerId(e.target.value)}
            onFocus={(e) => { e.target.style.borderColor = colors.primary; }}
            onBlur={(e) => { e.target.style.borderColor = colors.gray200; }}
          />
          <button
            style={{
              ...s.btnPrimary,
              ...(insightsLoading || !customerId ? s.btnDisabled : {}),
            }}
            disabled={insightsLoading || !customerId}
            onClick={handleGenerateInsights}
          >
            {insightsLoading ? <><Spinner /> Generating...</> : 'Generate Insights'}
          </button>
        </div>
        {insightsError && <div style={s.errorText}>{insightsError}</div>}
        {insightsResult && (
          <div
            style={{
              ...(insightsFlash ? { animation: 'flashGreen 0.8s ease' } : {}),
            }}
          >
            {insightsResult.length === 0 && (
              <div style={{ ...s.emptyState, padding: 16 }}>No insights generated.</div>
            )}
            {insightsResult.map((insight, i) => (
              <div key={i} style={{ ...s.insightCard, marginTop: i === 0 ? 20 : 0 }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 8 }}>
                  <span style={{ ...s.badge, background: colors.mixedBg, color: colors.mixed }}>
                    {insight.insightType || 'INSIGHT'}
                  </span>
                  <span style={{ fontSize: 15, fontWeight: 700, color: colors.gray900 }}>
                    {insight.title}
                  </span>
                </div>
                <p style={{ fontSize: 14, color: colors.gray700, lineHeight: 1.6, margin: '0 0 8px' }}>
                  {insight.description}
                </p>
                <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                  <span style={{ fontSize: 12, color: colors.gray500, fontWeight: 600 }}>
                    Confidence: {((insight.confidenceScore || 0) * 100).toFixed(0)}%
                  </span>
                </div>
                <ConfidenceBar value={insight.confidenceScore} />
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Recent Analyses Table */}
      <div style={s.card}>
        <div style={s.cardTitle}>
          <span style={s.cardTitleIcon}>📋</span>
          Recent Analyses
        </div>
        {recentAnalyses.length === 0 ? (
          <div style={s.emptyState}>No analyses yet</div>
        ) : (
          <div style={{ overflowX: 'auto' }}>
            <table style={s.table}>
              <thead>
                <tr>
                  <th style={s.th}>Session #</th>
                  <th style={s.th}>Sentiment</th>
                  <th style={s.th}>Score</th>
                  <th style={s.th}>Satisfaction</th>
                  <th style={s.th}>Escalation Risk</th>
                  <th style={s.th}>Analyzed At</th>
                </tr>
              </thead>
              <tbody>
                {recentAnalyses.map((a) => (
                  <tr key={a.id || a.sessionId}>
                    <td style={s.td}>#{a.sessionId}</td>
                    <td style={s.td}><SentimentBadge sentiment={a.sentimentType} /></td>
                    <td style={s.td}>{a.sentimentScore?.toFixed(2) ?? '—'}</td>
                    <td style={s.td}><Stars rating={a.customerSatisfactionScore || 0} /></td>
                    <td style={s.td}><RiskDot risk={a.escalationRisk} /></td>
                    <td style={s.td}>
                      {a.analyzedAt ? new Date(a.analyzedAt).toLocaleString() : '—'}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Recent Insights List */}
      <div style={s.card}>
        <div style={s.cardTitle}>
          <span style={s.cardTitleIcon}>🧠</span>
          Recent Customer Insights
        </div>
        {recentInsights.length === 0 ? (
          <div style={s.emptyState}>No recent insights</div>
        ) : (
          recentInsights.map((item, i) => (
            <div key={item.id || i} style={s.insightCard}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 8 }}>
                <span style={{ fontSize: 14, fontWeight: 700, color: colors.gray900 }}>
                  {item.userName || `Customer #${item.userId}`}
                </span>
                <span style={{ ...s.badge, background: colors.mixedBg, color: colors.mixed, fontSize: 11 }}>
                  {item.insightType || 'INSIGHT'}
                </span>
              </div>
              <div style={{ fontSize: 15, fontWeight: 600, color: colors.gray900, marginBottom: 4 }}>
                {item.title}
              </div>
              <p style={{ fontSize: 13, color: colors.gray500, lineHeight: 1.5, margin: '0 0 8px' }}>
                {item.description?.length > 150
                  ? item.description.slice(0, 150) + '…'
                  : item.description}
              </p>
              <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                <span style={{ fontSize: 12, color: colors.primary, fontWeight: 700 }}>
                  {((item.confidenceScore || 0) * 100).toFixed(0)}% confidence
                </span>
              </div>
              <ConfidenceBar value={item.confidenceScore} />
            </div>
          ))
        )}
      </div>
    </div>
  );
}
