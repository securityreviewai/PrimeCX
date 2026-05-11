import React, { useState, useEffect } from 'react';
import axios from 'axios';
import './SentimentTimeline.css';

/**
 * Customer Sentiment Timeline Component
 * Displays sentiment trends across customer interactions
 * Helps teams spot escalation early
 */
const SentimentTimeline = ({ customerId }) => {
  const [timeline, setTimeline] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [filters, setFilters] = useState({
    startDate: null,
    endDate: null,
    page: 0,
    pageSize: 20,
  });

  // Fetch sentiment timeline data
  useEffect(() => {
    if (!customerId) return;
    
    fetchSentimentTimeline();
  }, [customerId, filters]);

  const fetchSentimentTimeline = async () => {
    try {
      setLoading(true);
      setError(null);

      const params = {
        customerId,
        page: filters.page,
        pageSize: filters.pageSize,
      };

      if (filters.startDate) {
        params.startDate = filters.startDate.toISOString();
      }
      if (filters.endDate) {
        params.endDate = filters.endDate.toISOString();
      }

      // Validate customerId is a positive integer
      if (!Number.isInteger(customerId) || customerId <= 0) {
        setError('Invalid customer ID');
        return;
      }

      const response = await axios.get('/api/sentiment-timeline', { params });
      setTimeline(response.data);
    } catch (err) {
      console.error('Error fetching sentiment timeline:', err);
      // Display generic error message - don't expose backend details
      setError('Failed to load sentiment timeline. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const handleDateChange = (field, value) => {
    setFilters(prev => ({
      ...prev,
      [field]: value,
      page: 0, // Reset pagination on filter change
    }));
  };

  const handlePageChange = (newPage) => {
    setFilters(prev => ({
      ...prev,
      page: newPage,
    }));
  };

  const getSentimentColor = (sentimentType) => {
    const colors = {
      POSITIVE: '#10b981',   // Green
      NEUTRAL: '#6b7280',    // Gray
      NEGATIVE: '#ef4444',   // Red
      MIXED: '#f59e0b',      // Amber
    };
    return colors[sentimentType] || '#6b7280';
  };

  const getTrendIcon = (trend) => {
    if (trend === 'IMPROVING') return '📈';
    if (trend === 'DECLINING') return '📉';
    return '→';
  };

  if (loading) {
    return <div className="sentiment-timeline-container">Loading sentiment data...</div>;
  }

  if (error) {
    return <div className="sentiment-timeline-container error">{error}</div>;
  }

  if (!timeline) {
    return <div className="sentiment-timeline-container">No sentiment data available</div>;
  }

  return (
    <div className="sentiment-timeline-container">
      <div className="sentiment-header">
        <h2>Customer Sentiment Timeline</h2>
        <div className="sentiment-metrics">
          <div className="metric">
            <span className="metric-label">Average Sentiment:</span>
            <span className="metric-value">
              {timeline.averageSentimentScore ? (timeline.averageSentimentScore * 100).toFixed(0) : 'N/A'}%
            </span>
          </div>
          <div className="metric">
            <span className="metric-label">Trend:</span>
            <span className="metric-value">{getTrendIcon(timeline.sentimentTrend)} {timeline.sentimentTrend}</span>
          </div>
          {timeline.hasEscalationRisks && (
            <div className="metric escalation-warning">
              <span className="metric-label">⚠️ Escalation Risk Detected</span>
            </div>
          )}
        </div>
      </div>

      <div className="sentiment-filters">
        <input
          type="date"
          placeholder="Start Date"
          onChange={(e) => handleDateChange('startDate', e.target.valueAsDate)}
        />
        <input
          type="date"
          placeholder="End Date"
          onChange={(e) => handleDateChange('endDate', e.target.valueAsDate)}
        />
      </div>

      <div className="sentiment-entries">
        {timeline.entries && timeline.entries.length > 0 ? (
          timeline.entries.map((entry) => (
            <div key={entry.sessionId} className="sentiment-entry">
              <div className="entry-header">
                <div
                  className="sentiment-indicator"
                  style={{ backgroundColor: getSentimentColor(entry.sentimentType) }}
                  title={entry.sentimentType}
                />
                <div className="entry-time">
                  {entry.analyzedAt ? new Date(entry.analyzedAt).toLocaleString() : 'N/A'}
                </div>
                <div className="sentiment-type">{entry.sentimentType}</div>
                <div className="sentiment-score">
                  {entry.sentimentScore ? (entry.sentimentScore * 100).toFixed(0) : 'N/A'}%
                </div>
              </div>
              
              <div className="entry-details">
                {entry.summary && (
                  <div className="detail">
                    <span className="label">Summary:</span>
                    <span className="value">{entry.summary.substring(0, 200)}...</span>
                  </div>
                )}
                
                {entry.customerSatisfactionScore && (
                  <div className="detail">
                    <span className="label">CSAT:</span>
                    <span className="value">{(entry.customerSatisfactionScore * 100).toFixed(0)}%</span>
                  </div>
                )}
                
                {entry.escalationRisk && (
                  <div className="detail escalation">
                    <span className="label">⚠️ Risk:</span>
                    <span className="value">{entry.escalationRisk}</span>
                  </div>
                )}
              </div>
            </div>
          ))
        ) : (
          <div className="no-data">No sentiment data for this period</div>
        )}
      </div>

      <div className="sentiment-pagination">
        <button
          onClick={() => handlePageChange(filters.page - 1)}
          disabled={filters.page === 0}
        >
          Previous
        </button>
        <span>
          Page {filters.page + 1} of {Math.ceil(timeline.totalCount / filters.pageSize)}
        </span>
        <button
          onClick={() => handlePageChange(filters.page + 1)}
          disabled={(filters.page + 1) * filters.pageSize >= timeline.totalCount}
        >
          Next
        </button>
      </div>
    </div>
  );
};

export default SentimentTimeline;
