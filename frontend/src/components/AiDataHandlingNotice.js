import React, { useEffect, useState } from 'react';
import { getAiLlmDataHandling } from '../services/api';

const colors = {
  gray100: '#F3F4F6',
  gray200: '#E5E7EB',
  gray500: '#6B7280',
  gray700: '#374151',
  gray900: '#111827',
  success: '#059669',
  warning: '#D97706',
};

const styles = {
  wrap: { borderRadius: 12, border: `1px solid ${colors.gray200}`, background: colors.gray100, overflow: 'hidden' },
  header: {
    width: '100%',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    gap: 12,
    padding: '12px 16px',
    border: 'none',
    background: 'transparent',
    cursor: 'pointer',
    fontSize: 14,
    fontWeight: 700,
    color: colors.gray900,
    textAlign: 'left',
  },
  body: { padding: '0 16px 16px', borderTop: `1px solid ${colors.gray200}`, background: '#fff' },
  overview: { fontSize: 13, color: colors.gray700, lineHeight: 1.55, margin: '14px 0 12px' },
  sectionLabel: { fontSize: 12, fontWeight: 700, color: colors.gray500, textTransform: 'uppercase', letterSpacing: '0.04em', marginTop: 14, marginBottom: 6 },
  list: { margin: 0, paddingLeft: 18, fontSize: 13, color: colors.gray700, lineHeight: 1.5 },
  flow: {
    marginBottom: 10,
    padding: '10px 12px',
    borderRadius: 8,
    border: `1px solid ${colors.gray200}`,
    background: colors.gray100,
  },
  flowTitle: { fontWeight: 600, fontSize: 13, color: colors.gray900, marginBottom: 4 },
  badge: { display: 'inline-block', fontSize: 11, fontWeight: 700, padding: '2px 8px', borderRadius: 12, marginRight: 8 },
  err: { fontSize: 12, color: '#B91C1C', padding: 12 },
};

export default function AiDataHandlingNotice({ compact = false, initiallyOpen, enabled = true }) {
  const [open, setOpen] = useState(initiallyOpen ?? !compact);
  const [data, setData] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!enabled) {
      return undefined;
    }
    let cancelled = false;
    (async () => {
      try {
        setError(null);
        const res = await getAiLlmDataHandling();
        if (!cancelled) setData(res.data);
      } catch (e) {
        if (!cancelled) {
          if (e.response?.status === 403) {
            setError(null);
            setData(null);
          } else {
            setError('Could not load AI data-handling disclosure.');
          }
        }
      }
    })();
    return () => { cancelled = true; };
  }, [enabled]);

  if (!enabled) {
    return null;
  }

  if (error) {
    return <div style={styles.err}>{error}</div>;
  }
  if (!data) {
    return null;
  }

  const flows = data.flows || [];
  const rules = data.redactionRules || [];
  const trunc = data.truncationNotes || [];

  return (
    <div style={styles.wrap}>
      <button type="button" style={styles.header} onClick={() => setOpen((v) => !v)} aria-expanded={open}>
        <span>
          {compact ? 'AI & PII' : 'What we send to AI (PII-safe copy)'}
        </span>
        <span style={{ fontSize: 12, color: colors.gray500 }}>{open ? 'Hide' : 'Show'}</span>
      </button>
      {open && (
        <div style={styles.body}>
          <p style={styles.overview}>{data.overview}</p>

          <div style={styles.sectionLabel}>Redaction (AiPayloadRedactor)</div>
          <ul style={styles.list}>
            {rules.map((r, i) => (
              <li key={i}>
                {r.patternDescription} → <code style={{ fontSize: 12 }}>{r.replacementPlaceholder}</code>
              </li>
            ))}
          </ul>

          <div style={styles.sectionLabel}>Truncation</div>
          <ul style={styles.list}>
            {trunc.map((t, i) => (
              <li key={i}>{t.description}</li>
            ))}
          </ul>

          <div style={styles.sectionLabel}>By feature</div>
          {flows.map((f) => (
            <div key={f.flowKey} style={styles.flow}>
              <div style={{ marginBottom: 4 }}>
                <span
                  style={{
                    ...styles.badge,
                    background: f.payloadRedactionApplied ? `${colors.success}22` : `${colors.warning}22`,
                    color: f.payloadRedactionApplied ? colors.success : colors.warning,
                  }}
                >
                  {f.payloadRedactionApplied ? 'Redaction on' : 'No redaction'}
                </span>
                {typeof f.maxCharsSentToProvider === 'number' && (
                  <span style={{ fontSize: 11, color: colors.gray500 }}>
                    Up to {f.maxCharsSentToProvider.toLocaleString()} chars to model
                  </span>
                )}
              </div>
              <div style={styles.flowTitle}>{f.title}</div>
              <p style={{ fontSize: 13, color: colors.gray700, lineHeight: 1.5, margin: 0 }}>{f.detail}</p>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
